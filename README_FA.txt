Cloud Storage Explorer 1

نسخه تمیز اولیه برای Repository جدید.

امکانات پایه این نسخه:
- Add Cloud با Android Storage Access Framework
- پشتیبانی از Cloud Providerهای نصب‌شده که Android به‌عنوان Document Provider ارائه می‌کند؛ از جمله Google Drive، Dropbox، OneDrive و Box در صورت در دسترس بودن روی دستگاه
- نگهداری دسترسی انتخاب‌شده با Persistable URI Permission
- Share با استاندارد Android برای ارسال فایل به برنامه‌های ابری و سایر برنامه‌های نصب‌شده
- Internal Storage / SD Card / USB-OTG به‌عنوان ورودی‌های File Explorer
- Workflow مستقل GitHub Actions؛ بدون android-actions/setup-android@v3 و بدون پکیج منسوخ tools

نکته: اتصال مستقیم API به هر سرویس ابری (OAuth/API اختصاصی) مرحله جداگانه‌ای است. این نسخه از روش استاندارد Android استفاده می‌کند تا APK سبک بماند.
