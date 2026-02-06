# 🔧 راهنمای تنظیم Telemetry با Supabase

این راهنما نحوه اتصال سیستم Telemetry اپلیکیشن SCAMYNX به Supabase را توضیح می‌دهد.

---

## 📋 پیش‌نیازها

1. یک پروژه Supabase فعال
2. Supabase CLI نصب شده (اختیاری، برای deploy)
3. دسترسی به Dashboard پروژه

---

## مرحله ۱: ساخت جدول دیتابیس

در Supabase Dashboard به **SQL Editor** بروید و این query را اجرا کنید:

```sql
-- جدول اصلی برای ذخیره event ها
CREATE TABLE telemetry_events (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    session_id TEXT NOT NULL,
    event_type TEXT NOT NULL,
    timestamp TIMESTAMPTZ NOT NULL,
    payload JSONB DEFAULT '{}',
    app_version TEXT,
    device_manufacturer TEXT,
    device_model TEXT,
    android_version TEXT,
    batch_id TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- ایندکس‌ها برای کوئری سریع‌تر
CREATE INDEX idx_telemetry_event_type ON telemetry_events(event_type);
CREATE INDEX idx_telemetry_session ON telemetry_events(session_id);
CREATE INDEX idx_telemetry_timestamp ON telemetry_events(timestamp DESC);
CREATE INDEX idx_telemetry_created ON telemetry_events(created_at DESC);

-- فعال‌سازی Row Level Security
ALTER TABLE telemetry_events ENABLE ROW LEVEL SECURITY;

-- Policy برای اجازه insert (فقط از طریق Edge Function با service role)
CREATE POLICY "Service role can insert" ON telemetry_events
    FOR INSERT
    TO service_role
    WITH CHECK (true);

-- Policy برای خواندن (فقط admin ها)
CREATE POLICY "Admin can read" ON telemetry_events
    FOR SELECT
    TO authenticated
    USING (auth.jwt() ->> 'role' = 'admin');
```

---

## مرحله ۲: Deploy کردن Edge Function

### روش ۱: از طریق Dashboard

1. به **Edge Functions** در Dashboard بروید
2. روی **Create new function** کلیک کنید
3. نام: `telemetry`
4. کد فایل `docs/supabase/functions/telemetry/index.ts` را paste کنید
5. **Deploy** کنید

### روش ۲: از طریق CLI

```bash
# نصب Supabase CLI
npm install -g supabase

# لاگین
supabase login

# لینک به پروژه
supabase link --project-ref <YOUR_PROJECT_REF>

# Deploy
supabase functions deploy telemetry
```

---

## مرحله ۳: تنظیم در اپلیکیشن

در فایل `secrets.properties` این مقدار را تنظیم کنید:

```properties
SCAMYNX_TELEMETRY_ENDPOINT=https://<YOUR_PROJECT_REF>.supabase.co/functions/v1/telemetry
```

---

## 📊 ساختار داده‌ها

### Event Types (انواع رویدادها)

| Event Type | توضیح |
|------------|-------|
| `app_launched` | اپ باز شد |
| `scan_started` | اسکن شروع شد |
| `scan_completed` | اسکن تمام شد |
| `scan_failed` | اسکن با خطا مواجه شد |
| `feature_used` | یک قابلیت استفاده شد |
| `settings_changed` | تنظیمات تغییر کرد |
| `error_occurred` | خطا رخ داد |
| `threat_detected` | تهدید شناسایی شد |
| `report_generated` | گزارش تولید شد |
| `privacy_radar_started` | Privacy Radar شروع شد |
| `privacy_radar_stopped` | Privacy Radar متوقف شد |

### مثال Request (Single Event)

```json
POST /v1/telemetry/event
{
  "session_id": "550e8400-e29b-41d4-a716-446655440000",
  "event_type": "scan_completed",
  "timestamp": "2026-02-01T10:30:00Z",
  "payload": {
    "target_type": "url",
    "risk_level": "medium",
    "duration": "1500"
  },
  "app_version": "1.0.0-beta2",
  "device_info": {
    "manufacturer": "Samsung",
    "model": "Galaxy S24",
    "android_version": "14"
  }
}
```

### مثال Request (Batch)

```json
POST /v1/telemetry/batch
{
  "batch_id": "batch-001",
  "events": [
    {
      "session_id": "...",
      "event_type": "app_launched",
      "timestamp": "...",
      "payload": {}
    },
    {
      "session_id": "...",
      "event_type": "feature_used",
      "timestamp": "...",
      "payload": { "feature_name": "qr_scanner" }
    }
  ]
}
```

### Response

```json
{
  "status": "ok",
  "processed_events": 2,
  "batch_id": "batch-001"
}
```

---

## 📈 کوئری‌های مفید برای Analytics

### تعداد event ها بر اساس نوع

```sql
SELECT 
    event_type, 
    COUNT(*) as count 
FROM telemetry_events 
GROUP BY event_type 
ORDER BY count DESC;
```

### فعال‌ترین کاربران (بر اساس session)

```sql
SELECT 
    session_id, 
    COUNT(*) as events_count,
    MIN(timestamp) as first_seen,
    MAX(timestamp) as last_seen
FROM telemetry_events 
GROUP BY session_id 
ORDER BY events_count DESC 
LIMIT 20;
```

### نسخه‌های اپ در حال استفاده

```sql
SELECT 
    app_version, 
    COUNT(DISTINCT session_id) as users
FROM telemetry_events 
WHERE app_version IS NOT NULL
GROUP BY app_version 
ORDER BY users DESC;
```

### دستگاه‌های محبوب

```sql
SELECT 
    device_manufacturer,
    device_model,
    COUNT(DISTINCT session_id) as users
FROM telemetry_events 
WHERE device_manufacturer IS NOT NULL
GROUP BY device_manufacturer, device_model
ORDER BY users DESC
LIMIT 20;
```

### تعداد اسکن‌های موفق روزانه

```sql
SELECT 
    DATE(timestamp) as date,
    COUNT(*) as scans
FROM telemetry_events 
WHERE event_type = 'scan_completed'
GROUP BY DATE(timestamp)
ORDER BY date DESC
LIMIT 30;
```

### تهدیدات شناسایی شده

```sql
SELECT 
    payload->>'risk_level' as risk_level,
    COUNT(*) as count
FROM telemetry_events 
WHERE event_type = 'threat_detected'
GROUP BY payload->>'risk_level';
```

---

## 🔒 نکات امنیتی

1. **هرگز API Key را در اپ قرار ندهید** - Edge Function از Service Role Key استفاده می‌کند که سمت سرور است

2. **داده‌های حساس جمع نکنید** - URL ها و محتوای اسکن شده را ذخیره نکنید

3. **Rate Limiting** - می‌توانید در Edge Function محدودیت اضافه کنید

4. **Data Retention** - یک scheduled job برای پاک کردن داده‌های قدیمی بسازید:

```sql
-- پاک کردن event های قدیمی‌تر از ۹۰ روز
DELETE FROM telemetry_events 
WHERE created_at < NOW() - INTERVAL '90 days';
```

---

## ✅ تست کردن

پس از deploy، می‌توانید با curl تست کنید:

```bash
curl -X POST 'https://<YOUR_PROJECT_REF>.supabase.co/functions/v1/telemetry/v1/telemetry/batch' \
  -H 'Content-Type: application/json' \
  -d '{
    "batch_id": "test-001",
    "events": [{
      "session_id": "test-session",
      "event_type": "app_launched",
      "timestamp": "2026-02-01T10:00:00Z",
      "payload": {}
    }]
  }'
```

---

**آخرین بروزرسانی:** ۱۲ بهمن ۱۴۰۴
