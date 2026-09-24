#!/usr/bin/env bash
set -e

echo "============================================="
echo "  بناء تطبيقي توثيق (نظيف تماماً بدون كاش)  "
echo "============================================="

mkdir -p ./war7 ./production

echo "[1/2] تنظيف وبناء تطبيق المستخدم (User App: com.aistudio.tawthiq.kfyrt) ..."
gradle clean
gradle :app:assembleDebug -PtargetApp=user --no-build-cache
cp ./app/build/outputs/apk/debug/app-debug.apk ./war7/userapk.apk
cp ./app/build/outputs/apk/debug/app-debug.apk ./userapk.apk
cp ./app/build/outputs/apk/debug/app-debug.apk ./tawthiq.apk
cp ./app/build/outputs/apk/debug/app-debug.apk ./app-debug.apk
cp ./app/build/outputs/apk/debug/app-debug.apk ./production/userapk.apk

echo "✓ تم إنشاء تطبيق المستخدم بنجاح."

echo "[2/2] تنظيف وبناء تطبيق الإدارة (Admin App: com.aistudio.tawthiq.admin) ..."
gradle clean
gradle :app:assembleDebug -PtargetApp=admin --no-build-cache
cp ./app/build/outputs/apk/debug/app-debug.apk ./war7/adminapk.apk
cp ./app/build/outputs/apk/debug/app-debug.apk ./adminapk.apk
cp ./app/build/outputs/apk/debug/app-debug.apk ./tawthiq-admin.apk
cp ./app/build/outputs/apk/debug/app-debug.apk ./production/adminapk.apk

echo "✓ تم إنشاء تطبيق الإدارة بنجاح."

echo "============================================="
echo "  محتويات مجلد war7: "
ls -lh ./war7/
echo "============================================="
