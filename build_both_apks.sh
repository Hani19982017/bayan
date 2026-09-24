#!/usr/bin/env bash
set -e

echo "============================================="
echo "  بناء تطبيقي توثيق (المستخدم والإدارة)  "
echo "============================================="

echo "[1/4] بناء تطبيق المستخدم (User App) ..."
gradle :app:assembleDebug -PtargetApp=user
cp ./app/build/outputs/apk/debug/app-debug.apk ./userapk.apk
cp ./app/build/outputs/apk/debug/app-debug.apk ./tawthiq-user.apk
cp ./app/build/outputs/apk/debug/app-debug.apk ./tawthiq.apk
cp ./app/build/outputs/apk/debug/app-debug.apk ./app-debug.apk
echo "✓ تم إنشاء تطبيق المستخدم: userapk.apk و tawthiq-user.apk"

echo "[2/4] بناء تطبيق الإدارة (Admin App) ..."
gradle :app:assembleDebug -PtargetApp=admin
cp ./app/build/outputs/apk/debug/app-debug.apk ./adminapk.apk
cp ./app/build/outputs/apk/debug/app-debug.apk ./tawthiq-admin.apk
cp ./app/build/outputs/apk/debug/app-debug.apk ./admin.apk
echo "✓ تم إنشاء تطبيق الإدارة: adminapk.apk و tawthiq-admin.apk"

echo "============================================="
echo "  الملفات الجاهزة في المجلد الرئيسي: "
ls -lh userapk.apk adminapk.apk tawthiq-user.apk tawthiq-admin.apk
echo "============================================="
