# Fruits Warehouse App — Android MVP Source

## इस संस्करण में शामिल
- Android ऐप का Kotlin + Jetpack Compose source
- एक डिवाइस पर लोकल सेविंग
- Opening Stock / नया Lot
- Lot-wise Stock सूची
- Inward और Outward Carton movement
- Physical Count बनाम Software Stock का अंतर
- CSV Import/Export (CSV को Excel में खोला जा सकता है)
- Product Code, Fruit, Brand, Count, Size/Calibre, Weight per Carton, Room, Block, Cartons, Pallets
- उपलब्ध स्टॉक से अधिक Outward रोकना
- Duplicate Lot Number को रोकना

## अभी शामिल नहीं / सीमाएँ
- यह source code है, compiled APK नहीं। इस वातावरण में Android build/test नहीं चलाया गया।
- Photo AI counting अभी placeholder/अगले चरण का feature है।
- सीधे `.xlsx` पढ़ना शामिल नहीं; CSV export/import इस्तेमाल करें या बाद में XLSX parser जोड़ें।
- Inward/Outward transaction history, Room capacity limits, audit log, encrypted backup और cloud sync production संस्करण में जोड़ने होंगे।
- डेटा SharedPreferences में लोकल है; ऐप डेटा clear/uninstall करने से पहले export/backup करें।

## Build करने के लिए
1. Android Studio में `FruitsWarehouseApp` folder खोलें।
2. Gradle sync पूरा होने दें (Android SDK 35 और internet dependency download चाहिए)।
3. Android device/emulator पर Run दबाएँ।
4. वास्तविक उपयोग से पहले test data पर सभी flows जाँचें और CSV backup लें।

## CSV Header
`lot,productCode,fruit,brand,count,size,kgPerCarton,room,block,cartons,pallets`

एक Lot में एक Brand और एक Count रखने के लिए हर अलग Brand/Count combination का अलग Lot Number इस्तेमाल करें।
