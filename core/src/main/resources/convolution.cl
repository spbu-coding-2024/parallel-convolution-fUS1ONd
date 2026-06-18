// Включаем поддержку double — нужно для коэффициентов ядра (Kernel.data: DoubleArray).
#pragma OPENCL EXTENSION cl_khr_fp64 : enable

// Свёртка одного пикселя на GPU. Запускается на сетке width × height,
// каждый work-item считает один пиксель результата.
// Соответствие CPU-версии (Convolution.kt):
//   result(x, y) = clamp(factor * Σ image(x+kx, y+ky) * kernel(kx, ky) + bias, 0, 255)
// Обработка границ — CLAMP (координата клампится к [0, width-1] / [0, height-1]).
__kernel void convolve(
    __global const int* input,
    __global int* output,
    __global const double* kernelData,
    const int width,
    const int height,
    const int radius,
    const double factor,
    const double bias
) {
    const int x = get_global_id(0);
    const int y = get_global_id(1);

    // Защита от запуска на сетке, большей чем картинка (округление вверх в host-коде).
    if (x >= width || y >= height) return;

    const int kSize = 2 * radius + 1;
    double sum = 0.0;

    for (int ky = -radius; ky <= radius; ++ky) {
        for (int kx = -radius; kx <= radius; ++kx) {
            const int sx = clamp(x + kx, 0, width - 1);
            const int sy = clamp(y + ky, 0, height - 1);
            const int pixel = input[sy * width + sx];
            const double coeff = kernelData[(ky + radius) * kSize + (kx + radius)];
            sum += pixel * coeff;
        }
    }

    int value = (int)(sum * factor + bias);
    value = clamp(value, 0, 255);
    output[y * width + x] = value;
}
