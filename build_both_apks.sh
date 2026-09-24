#!/usr/bin/env bash
set -e

echo "============================================="
echo "  بناء تطبيقي البيان / توثيق (المستخدم والإدارة)  "
echo "============================================="

mkdir -p ./production ./prouction

echo "[1/4] بناء تطبيق المستخدم (User App) ..."
gradle :app:assembleDebug -PtargetApp=user
cp ./app/build/outputs/apk/debug/app-debug.apk ./userapk.apk
cp ./app/build/outputs/apk/debug/app-debug.apk ./tawthiq-user.apk
cp ./app/build/outputs/apk/debug/app-debug.apk ./tawthiq.apk
cp ./app/build/outputs/apk/debug/app-debug.apk ./app-debug.apk
cp ./app/build/outputs/apk/debug/app-debug.apk ./production/userapk.apk
cp ./app/build/outputs/apk/debug/app-debug.apk ./production/production.apk
cp ./app/build/outputs/apk/debug/app-debug.apk ./prouction/userapk.apk
echo "✓ تم إنشاء تطبيق المستخدم: userapk.apk و tawthiq-user.apk"

echo "[2/4] بناء تطبيق الإدارة (Admin App) ..."
gradle :app:assembleDebug -PtargetApp=admin
cp ./app/build/outputs/apk/debug/app-debug.apk ./adminapk.apk
cp ./app/build/outputs/apk/debug/app-debug.apk ./tawthiq-admin.apk
cp ./app/build/outputs/apk/debug/app-debug.apk ./admin.apk
cp ./app/build/outputs/apk/debug/app-debug.apk ./production/adminapk.apk
cp ./app/build/outputs/apk/debug/app-debug.apk ./production/admin.apk
cp ./app/build/outputs/apk/debug/app-debug.apk ./prouction/adminapk.apk
echo "✓ تم إنشاء تطبيق الإدارة: adminapk.apk و tawthiq-admin.apk"

echo "============================================="
echo "  الملفات الجاهزة في المجلد الرئيسي و production: "
ls -lh userapk.apk adminapk.apk ./production/userapk.apk ./production/adminapk.apk
echo "============================================="
