package workshop.parallels.core

// Стратегия обработки пикселей за границей изображения при свёртке.
// Сейчас реализована только CLAMP (значение ближайшего крайнего пикселя):
// этого достаточно для всех property-тестов из ТЗ. Enum используется
// для расширяемости — при необходимости легко добавить ZERO, MIRROR, WRAP
// без изменений в API свёртки.
enum class BorderStrategy {
    CLAMP {
        override fun getPixel(image: Image, x: Int, y: Int): Int {
            val cx = x.coerceIn(0, image.width - 1)
            val cy = y.coerceIn(0, image.height - 1)
            return image.get(cx, cy)
        }
    }, ;

    abstract fun getPixel(image: Image, x: Int, y: Int): Int
}
