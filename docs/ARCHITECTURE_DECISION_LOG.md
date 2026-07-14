بله. قبل از هر تغییر معماری جدید، بهترین کار همین است که یک **Decision Document** داشته باشیم تا از اینجا به بعد بر اساس تصمیم‌های تثبیت‌شده جلو برویم.

با توجه به وضعیت فعلی پروژه (بعد از فاز ۳)، پیشنهاد می‌کنم یک سند مثل:

`docs/ARCHITECTURE_DECISION_LOG.md`

ایجاد کنیم.

هدف این سند این نیست که راهکار را تحمیل کند؛ فقط تمام نقاطی که نیاز به تصمیم معماری دارند را مشخص می‌کند، گزینه‌ها، مزایا، معایب و اثرات آینده را ثبت می‌کند.

نسخه اولیه سند:

---

# Architecture Decision Log — Post Phase 3

## هدف سند

این سند تمام تصمیم‌های معماری مهم Vision Planner را قبل از ورود به فازهای بعدی ثبت می‌کند.

وضعیت فعلی پروژه:

* Goal → Task → Event معماری اصلی تثبیت شده است.
* goalId به عنوان ارتباط اصلی Goal و Task انتخاب شده است.
* Insight logic از ViewModel جدا شده است.
* Goal Event tracking اضافه شده است.
* Repository boundary در بخش‌های اصلی ایجاد شده است.
* تست‌های پایه ایجاد شده‌اند.
* معماری هنوز Offline First است.
* AI هنوز وارد معماری نشده است.

---

# Decision Area 1 — Graph Architecture

## وضعیت فعلی

ساختار داده فعلی:

```
Goal
 |
 └── Task
       |
       └── TaskEvent
```

همچنین:

```
Task
 |
 └── LifeArea
```

وجود دارد.

این ساختار برای ایجاد Graph مناسب است.

---

## سوال تصمیم‌گیری

آیا Graph باید:

### Option A — فقط یک View Projection باشد

یعنی Graph فقط هنگام نمایش ساخته شود.

مثال:

Database:

```
Goal Table
Task Table
LifeArea Table
```

↓

Graph Builder

↓

```
Goal Node
Task Node
LifeArea Node
```

مزایا:

* ساده‌تر
* بدون migration
* بدون duplication
* مناسب برای تعداد داده شخصی

معایب:

* محاسبات پیچیده‌تر در آینده
* هر بار باید graph ساخته شود

---

### Option B — Graph به عنوان Domain Model دائمی باشد

یعنی ارتباطات Graph ذخیره شوند.

مثلا:

```
graph_nodes

graph_edges
```

مزایا:

* سریع برای visualization
* مناسب برای AI reasoning
* مناسب برای تحلیل رابطه‌ها

معایب:

* دو source of truth ایجاد می‌شود
* sync مشکل می‌شود
* پیچیدگی بالا می‌رود

---

## تصمیم مورد نیاز:

آیا Graph:

[ ] فقط یک representation از داده‌های فعلی باشد

یا

[ ] یک مدل مستقل ذخیره‌شده باشد؟

---

# Decision Area 2 — Life Area Architecture

## وضعیت فعلی

LifeArea الان بیشتر یک tag برای Task است.

مثلا:

```
Task
 |
 lifeAreaId
```

اما Vision Planner می‌تواند Life Area را به یک مفهوم اصلی تبدیل کند.

---

## سوال تصمیم‌گیری

Life Area چه نقشی دارد؟

---

## Option A — دسته‌بندی ساده

مثال:

```
Health
Career
Learning
Finance
```

Task فقط متعلق به یک دسته است.

مزایا:

* ساده
* قابل فهم
* مناسب نسخه فعلی

معایب:

* قدرت تحلیلی کم

---

## Option B — Domain Entity مستقل

یعنی:

```
LifeArea

id
name
description
priority
target
createdAt
```

و:

```
Goal
 |
 LifeArea
 |
 Task
```

مزایا:

* مناسب Goal Management
* مناسب Progress
* مناسب Balance Analysis

معایب:

* نیاز به طراحی بیشتر

---

## Option C — Life Area به عنوان محور اصلی سیستم

ساختار:

```
Life Area

   |
   Goal

       |
       Task

           |
           Event
```

یعنی کاربر اول زندگی را تعریف می‌کند، بعد هدف‌ها را.

مزایا:

* بسیار نزدیک به مفهوم Personal Operating System

معایب:

* تغییر بزرگ UX

---

## تصمیم مورد نیاز:

Life Area:

[ ] فقط Tag

[ ] Entity مستقل

[ ] محور اصلی سیستم

---

# Decision Area 3 — Progress Model

## وضعیت فعلی

Progress الان محاسبه می‌شود.

مثلا:

```
completed tasks / total tasks
```

اما history نداریم.

---

## سوال تصمیم

آیا Progress باید Snapshot داشته باشد؟

مثال:

```
GoalProgressSnapshot

goalId
date
completedCount
totalCount
percentage
```

---

## Option A

محاسبه لحظه‌ای

مزایا:

* ساده
* بدون migration

معایب:

* Trend سخت
* Graph محدود

---

## Option B

ذخیره Snapshot

مزایا:

* نمودار زمانی
* تحلیل رفتار
* AI context در آینده

معایب:

* نیاز به aggregation

---

## تصمیم مورد نیاز:

Progress:

[ ] فقط Live Calculation

[ ] Historical Snapshot

---

# Decision Area 4 — Behavioral Analytics

## وضعیت فعلی

اطلاعات رفتاری داریم:

* completion rate
* streak
* reschedule
* velocity
* neglected goals

اما هنوز مدل مستقلی نداریم.

---

## سوال تصمیم

آیا Behavior باید Entity مستقل شود؟

مثلا:

```
BehaviorSnapshot

date
focusScore
consistencyScore
completionRate
```

---

## Option A

محاسبه مستقیم از Eventها

مزایا:

* ساده

معایب:

* تکرار محاسبات

---

## Option B

Behavior Store

مزایا:

* سریع
* آماده AI آینده

معایب:

* نیاز به pipeline

---

## تصمیم مورد نیاز:

Behavior:

[ ] فقط از Event استخراج شود

[ ] ذخیره شود

---

# Decision Area 5 — AI Strategy (فعلا تصمیم عدم اجرا)

## وضعیت فعلی

AI هنوز نباید وارد شود.

دلایل:

* Domain هنوز تثبیت نشده
* Graph مشخص نیست
* Behavior model کامل نیست
* Context generation تعریف نشده

---

## تصمیم فعلی پیشنهادی:

AI Implementation:

❌ Deferred

اما باید فقط یک قرارداد داشته باشیم:

```
InsightProvider

generateInsight(context)
```

بدون implementation.

---

# Decision Area 6 — User Model

## وضعیت فعلی

Single user.

---

## سوال:

آیا User Entity اضافه شود؟

مثلا:

```
User

id
settings
createdAt
```

---

مزیت:

* آینده Sync
* Multi profile
* Cloud

معایب:

* پیچیدگی بدون استفاده فعلی

---

## تصمیم مورد نیاز:

User:

[ ] فعلا اضافه نشود

[ ] از الان اضافه شود

---

# Decision Area 7 — Event Retention

## وضعیت فعلی

Eventها append-only هستند.

مشکل:

بعد از سال‌ها:

```
task_events
```

بزرگ می‌شود.

---

## تصمیم:

آیا:

[ ] همه Eventها همیشه نگه داشته شوند

یا

[ ] بعد از Snapshot حذف شوند

---

# پیشنهاد ترتیب بررسی

به نظر من این ترتیب منطقی است:

1. **Life Area Architecture**

چون روی Goal و Graph اثر مستقیم دارد.

2. **Graph Architecture**

چون مدل آینده محصول را تعیین می‌کند.

3. **Progress Model**

چون Graph و Trend به آن وابسته‌اند.

4. **Behavior Analytics**

چون بعد از Progress معنا پیدا می‌کند.

5. **User Model**

6. **Event Retention**

7. **AI**

(آخرین مورد)

---

این سند فعلاً فقط «مسائل تصمیم‌گیری» را ثبت می‌کند، نه راه‌حل نهایی.

قدم بعدی این است که از **Decision Area 1 یا 2** شروع کنیم و هرکدام را با توجه به Vision اصلی Vision Planner بررسی کنیم و تصمیم نهایی را ثبت کنیم.

