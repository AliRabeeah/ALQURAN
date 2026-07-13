package com.minshawi.quran

data class Surah(
    val number: Int,
    val arabicName: String
) {
    /** رقم السورة بصيغة 3 خانات مثل 001 */
    val paddedNumber: String get() = number.toString().padStart(3, '0')

    /** رابط التحميل المباشر من موقع mp3quran.net (قارئ المنشاوي - مرتل) */
    val downloadUrl: String get() =
        "https://server10.mp3quran.net/download/minsh/$paddedNumber.mp3"

    /** اسم الملف المحلي بعد التحميل */
    val fileName: String get() = "minsh_$paddedNumber.mp3"
}

object QuranData {
    val surahs: List<Surah> = listOf(
        Surah(1, "الفاتحة"), Surah(2, "البقرة"), Surah(3, "آل عمران"), Surah(4, "النساء"),
        Surah(5, "المائدة"), Surah(6, "الأنعام"), Surah(7, "الأعراف"), Surah(8, "الأنفال"),
        Surah(9, "التوبة"), Surah(10, "يونس"), Surah(11, "هود"), Surah(12, "يوسف"),
        Surah(13, "الرعد"), Surah(14, "إبراهيم"), Surah(15, "الحجر"), Surah(16, "النحل"),
        Surah(17, "الإسراء"), Surah(18, "الكهف"), Surah(19, "مريم"), Surah(20, "طه"),
        Surah(21, "الأنبياء"), Surah(22, "الحج"), Surah(23, "المؤمنون"), Surah(24, "النور"),
        Surah(25, "الفرقان"), Surah(26, "الشعراء"), Surah(27, "النمل"), Surah(28, "القصص"),
        Surah(29, "العنكبوت"), Surah(30, "الروم"), Surah(31, "لقمان"), Surah(32, "السجدة"),
        Surah(33, "الأحزاب"), Surah(34, "سبأ"), Surah(35, "فاطر"), Surah(36, "يس"),
        Surah(37, "الصافات"), Surah(38, "ص"), Surah(39, "الزمر"), Surah(40, "غافر"),
        Surah(41, "فصلت"), Surah(42, "الشورى"), Surah(43, "الزخرف"), Surah(44, "الدخان"),
        Surah(45, "الجاثية"), Surah(46, "الأحقاف"), Surah(47, "محمد"), Surah(48, "الفتح"),
        Surah(49, "الحجرات"), Surah(50, "ق"), Surah(51, "الذاريات"), Surah(52, "الطور"),
        Surah(53, "النجم"), Surah(54, "القمر"), Surah(55, "الرحمن"), Surah(56, "الواقعة"),
        Surah(57, "الحديد"), Surah(58, "المجادلة"), Surah(59, "الحشر"), Surah(60, "الممتحنة"),
        Surah(61, "الصف"), Surah(62, "الجمعة"), Surah(63, "المنافقون"), Surah(64, "التغابن"),
        Surah(65, "الطلاق"), Surah(66, "التحريم"), Surah(67, "الملك"), Surah(68, "القلم"),
        Surah(69, "الحاقة"), Surah(70, "المعارج"), Surah(71, "نوح"), Surah(72, "الجن"),
        Surah(73, "المزمل"), Surah(74, "المدثر"), Surah(75, "القيامة"), Surah(76, "الإنسان"),
        Surah(77, "المرسلات"), Surah(78, "النبأ"), Surah(79, "النازعات"), Surah(80, "عبس"),
        Surah(81, "التكوير"), Surah(82, "الإنفطار"), Surah(83, "المطففين"), Surah(84, "الإنشقاق"),
        Surah(85, "البروج"), Surah(86, "الطارق"), Surah(87, "الأعلى"), Surah(88, "الغاشية"),
        Surah(89, "الفجر"), Surah(90, "البلد"), Surah(91, "الشمس"), Surah(92, "الليل"),
        Surah(93, "الضحى"), Surah(94, "الشرح"), Surah(95, "التين"), Surah(96, "العلق"),
        Surah(97, "القدر"), Surah(98, "البينة"), Surah(99, "الزلزلة"), Surah(100, "العاديات"),
        Surah(101, "القارعة"), Surah(102, "التكاثر"), Surah(103, "العصر"), Surah(104, "الهمزة"),
        Surah(105, "الفيل"), Surah(106, "قريش"), Surah(107, "الماعون"), Surah(108, "الكوثر"),
        Surah(109, "الكافرون"), Surah(110, "النصر"), Surah(111, "المسد"), Surah(112, "الإخلاص"),
        Surah(113, "الفلق"), Surah(114, "الناس")
    )
}
