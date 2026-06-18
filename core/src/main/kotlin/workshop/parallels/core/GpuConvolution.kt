package workshop.parallels.core

import org.jocl.CL
import org.jocl.CL.CL_CONTEXT_PLATFORM
import org.jocl.CL.CL_DEVICE_TYPE_ALL
import org.jocl.CL.CL_MEM_COPY_HOST_PTR
import org.jocl.CL.CL_MEM_READ_ONLY
import org.jocl.CL.CL_MEM_WRITE_ONLY
import org.jocl.CL.CL_TRUE
import org.jocl.CL.clBuildProgram
import org.jocl.CL.clCreateBuffer
import org.jocl.CL.clCreateCommandQueueWithProperties
import org.jocl.CL.clCreateContext
import org.jocl.CL.clCreateKernel
import org.jocl.CL.clCreateProgramWithSource
import org.jocl.CL.clEnqueueNDRangeKernel
import org.jocl.CL.clEnqueueReadBuffer
import org.jocl.CL.clGetDeviceIDs
import org.jocl.CL.clGetPlatformIDs
import org.jocl.CL.clReleaseCommandQueue
import org.jocl.CL.clReleaseContext
import org.jocl.CL.clReleaseKernel
import org.jocl.CL.clReleaseMemObject
import org.jocl.CL.clReleaseProgram
import org.jocl.CL.clSetKernelArg
import org.jocl.Pointer
import org.jocl.Sizeof
import org.jocl.cl_context_properties
import org.jocl.cl_device_id
import org.jocl.cl_platform_id

// GPU-свёртка через OpenCL (JOCL). Семантика идентична convolve():
// CLAMP-граница, тот же результат с точностью до округления.
// Граница — только CLAMP (зашита в convolution.cl).
fun convolveGpu(image: Image, kernel: Kernel): Image {
    val width = image.width
    val height = image.height
    val source = loadKernelSource()

    // Инициализация OpenCL: платформа → устройство → контекст → очередь.
    CL.setExceptionsEnabled(true)
    val platform = firstPlatform()
    val device = firstDevice(platform)
    val context = clCreateContext(platformProps(platform), 1, arrayOf(device), null, null, null)
    val queue = clCreateCommandQueueWithProperties(context, device, null, null)

    // Программа из .cl-файла и kernel-функция.
    val program = clCreateProgramWithSource(context, 1, arrayOf(source), null, null)
    clBuildProgram(program, 0, null, null, null, null)
    val clKernel = clCreateKernel(program, "convolve", null)

    // Буферы: input и kernel-data — read-only с копированием с host; output — write-only.
    val inputBuf =
        clCreateBuffer(
            context,
            CL_MEM_READ_ONLY or CL_MEM_COPY_HOST_PTR,
            Sizeof.cl_int.toLong() * image.pixels.size,
            Pointer.to(image.pixels),
            null,
        )
    val kernelBuf =
        clCreateBuffer(
            context,
            CL_MEM_READ_ONLY or CL_MEM_COPY_HOST_PTR,
            Sizeof.cl_double.toLong() * kernel.data.size,
            Pointer.to(kernel.data),
            null,
        )
    val outputBuf =
        clCreateBuffer(
            context,
            CL_MEM_WRITE_ONLY,
            Sizeof.cl_int.toLong() * image.pixels.size,
            null,
            null,
        )

    val result = IntArray(image.pixels.size)
    try {
        // Аргументы ядра — в том же порядке, что в convolve(...) внутри .cl.
        clSetKernelArg(clKernel, 0, Sizeof.cl_mem.toLong(), Pointer.to(inputBuf))
        clSetKernelArg(clKernel, 1, Sizeof.cl_mem.toLong(), Pointer.to(outputBuf))
        clSetKernelArg(clKernel, 2, Sizeof.cl_mem.toLong(), Pointer.to(kernelBuf))
        clSetKernelArg(clKernel, 3, Sizeof.cl_int.toLong(), Pointer.to(intArrayOf(width)))
        clSetKernelArg(clKernel, 4, Sizeof.cl_int.toLong(), Pointer.to(intArrayOf(height)))
        clSetKernelArg(clKernel, 5, Sizeof.cl_int.toLong(), Pointer.to(intArrayOf(kernel.radius)))
        clSetKernelArg(clKernel, 6, Sizeof.cl_double.toLong(), Pointer.to(doubleArrayOf(kernel.factor)))
        clSetKernelArg(clKernel, 7, Sizeof.cl_double.toLong(), Pointer.to(doubleArrayOf(kernel.bias)))

        // Запуск ядра на двумерной сетке width × height — по одному work-item на пиксель.
        val globalSize = longArrayOf(width.toLong(), height.toLong())
        clEnqueueNDRangeKernel(queue, clKernel, 2, null, globalSize, null, 0, null, null)

        // Чтение результата (CL_TRUE — блокирующее, гарантирует завершение ядра).
        clEnqueueReadBuffer(
            queue,
            outputBuf,
            CL_TRUE,
            0,
            Sizeof.cl_int.toLong() * result.size,
            Pointer.to(result),
            0,
            null,
            null,
        )
    } finally {
        clReleaseMemObject(inputBuf)
        clReleaseMemObject(kernelBuf)
        clReleaseMemObject(outputBuf)
        clReleaseKernel(clKernel)
        clReleaseProgram(program)
        clReleaseCommandQueue(queue)
        clReleaseContext(context)
    }
    return Image(width, height, result)
}

private fun loadKernelSource(): String = object {}
    .javaClass
    .getResourceAsStream("/convolution.cl")
    ?.bufferedReader()
    ?.use { it.readText() }
    ?: error("convolution.cl не найден в ресурсах")

private fun firstPlatform(): cl_platform_id {
    val count = IntArray(1)
    clGetPlatformIDs(0, null, count)
    require(count[0] > 0) { "OpenCL-платформы не найдены" }
    val platforms = arrayOfNulls<cl_platform_id>(count[0])
    clGetPlatformIDs(count[0], platforms, null)
    return checkNotNull(platforms[0]) { "OpenCL вернул null-платформу при count=${count[0]}" }
}

private fun firstDevice(platform: cl_platform_id): cl_device_id {
    val count = IntArray(1)
    clGetDeviceIDs(platform, CL_DEVICE_TYPE_ALL, 0, null, count)
    require(count[0] > 0) { "OpenCL-устройства не найдены на платформе" }
    val devices = arrayOfNulls<cl_device_id>(count[0])
    clGetDeviceIDs(platform, CL_DEVICE_TYPE_ALL, count[0], devices, null)
    return checkNotNull(devices[0]) { "OpenCL вернул null-устройство при count=${count[0]}" }
}

private fun platformProps(platform: cl_platform_id): cl_context_properties =
    cl_context_properties().apply { addProperty(CL_CONTEXT_PLATFORM.toLong(), platform) }
