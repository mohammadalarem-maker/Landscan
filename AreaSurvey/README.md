# Area Survey - تطبيق مسح المساحات

تطبيق أندرويد لمسح المساحات بطريقتين، يستخدم خرائط OpenStreetMap (osmdroid) - مجاني تماماً بدون مفتاح API أو فوترة.

## 1. مسح بالمشي (Walking Survey)
- امشِ حول حدود الأرض، يسجل التطبيق نقاط GPS تلقائياً (كل ثانيتين) أو يدوياً بالضغط على "تسجيل نقطة"
- يرسم مضلع الأرض على الخريطة مباشرة
- يحسب المساحة بـ: م²، هكتار، دونم، فدان
- يحسب المحيط الكلي

## 2. محاكاة Total Station
- حدد نقطة وقوفك (Station) بالـ GPS
- وجّه الهاتف نحو كل نقطة هدف، أدخل المسافة، والتطبيق يقرأ الزاوية من البوصلة تلقائياً (أو أدخلها يدوياً)
- يحسب موقع كل نقطة هدف ويرسمها على الخريطة مع خطوط من نقطة الوقوف
- يحسب المساحة والمحيط من النقاط المُجمّعة

## البناء

```bash
./gradlew assembleDebug
```

لا توجد أي إعدادات إضافية مطلوبة (لا مفاتيح API، لا فوترة). الخرائط تُحمّل من خوادم OpenStreetMap (Mapnik) مباشرة عبر الإنترنت.

## بنية المشروع

```
app/src/main/java/com/survey/areasurvey/
├── MainActivity.kt
├── data/                  # Room: Entity, DAO, Database, TypeConverters
├── location/              # FusedLocationProviderClient wrapper (GPS)
├── sensors/               # CompassManager (Accelerometer + Magnetometer)
├── utils/GeoMath.kt       # حساب المساحة (Spherical) والمسافة والزاوية - صيغ يدوية
├── viewmodel/             # SurveyViewModel - كل المنطق
└── ui/                    # شاشات Compose (Home, Walking, TotalStation) + osmdroid MapView
```

## ملاحظات الدقة

- GPS العادي: دقته 3-5 متر، مناسب لقطع أرض متوسطة وكبيرة، غير دقيق لمساحات صغيرة جداً
- البوصلة (Azimuth): دقتها ±5-10 درجة وتتأثر بالحقول المغناطيسية القريبة (معادن، أسلاك كهرباء)
- يوجد حقل magneticDeclination في الـ ViewModel لتصحيح الانحراف المغناطيسي حسب الموقع (يمكن إضافة واجهة لإدخاله لاحقاً)
- خرائط OpenStreetMap تحتاج اتصال إنترنت لتحميل البلاطات (Tiles)؛ يمكن إضافة دعم Offline Maps لاحقاً عبر ملفات MBTiles

## التوسعات المقترحة لاحقاً
- شاشة "المشاريع المحفوظة" لعرض/حذف/تصدير المشاريع من Room
- تصدير CSV/KML
- معايرة Compass تلقائية (Calibration prompt)
- دعم Offline Maps (MBTiles)
