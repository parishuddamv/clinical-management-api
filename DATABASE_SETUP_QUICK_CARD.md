# 🚀 DATABASE SETUP - QUICK START CARD

## ⚡ Run These Commands (Copy-Paste Ready)

### Windows (PowerShell):
```powershell
cd D:\jusun\clinical-management-system

# Step 1: Extensions & Permissions
psql -U clinicos_user -d clinicos_db -f init-db/01-init.sql

# Step 2: Create Demo Booking Tables
bash init-db/init-tables.sh

# Step 3: Migration & Triggers
psql -U clinicos_user -d clinicos_db -f init-db/migration-demo-booking.sql

# Step 4: Performance Indexes
psql -U clinicos_user -d clinicos_db -f init-db/02-create-performance-indexes.sql

# Verify
psql -U clinicos_user -d clinicos_db -c "SELECT COUNT(*) FROM clinic_users;"
```

---

## 📝 What Each Script Does

| # | Script | Creates | Indexes | Time |
|---|--------|---------|---------|------|
| 1️⃣ | 01-init.sql | Extensions, Permissions | - | < 5s |
| 2️⃣ | init-tables.sh | clinic_users, demo_bookings | 6 | < 2s |
| 3️⃣ | migration-demo-booking.sql | Columns, Triggers | - | < 3s |
| 4️⃣ | 02-create-performance-indexes.sql | Performance | 25+ | < 5s |

**Total Time**: ~15 seconds

---

## 🎯 Tables Created

```
clinic_users                    demo_bookings
├── id                          ├── id
├── email ✅ UNIQUE             ├── email
├── full_name                   ├── full_name
├── role                        ├── role
├── status ✅ KEY               ├── status ✅ KEY
├── clinic_id                   ├── clinic_id
├── is_active                   ├── demo_date
├── approved_at                 ├── demo_time
├── approved_by                 ├── demo_timezone
├── last_login                  ├── specialization
└── timestamps                  └── timestamps
```

---

## 📊 Before & After

### Before Running Scripts ❌
```
❌ No clinic_users table
❌ No demo_bookings table
❌ No indexes
❌ No triggers
❌ No demo booking feature
```

### After Running Scripts ✅
```
✅ clinic_users table (16 columns, indexed)
✅ demo_bookings table (21 columns, indexed)
✅ 6 demo booking indexes
✅ 25+ performance indexes
✅ Auto-timestamp triggers
✅ Full demo booking feature ready
✅ Dashboard performance optimized
```

---

## 🔍 Verify It Worked

```bash
# Check tables exist
psql -U clinicos_user -d clinicos_db -c "\dt"

# Check indexes
psql -U clinicos_user -d clinicos_db -c "\di"

# Check data
psql -U clinicos_user -d clinicos_db -c "SELECT COUNT(*) as users FROM clinic_users; SELECT COUNT(*) as bookings FROM demo_bookings;"
```

---

## ✅ Success Criteria

- [ ] 01-init.sql ran successfully
- [ ] init-tables.sh ran successfully  
- [ ] migration-demo-booking.sql ran successfully
- [ ] 02-create-performance-indexes.sql ran successfully
- [ ] clinic_users table exists & has columns
- [ ] demo_bookings table exists & has columns
- [ ] All indexes created
- [ ] Can query the tables

---

## 🚨 Common Issues & Fixes

| Issue | Fix |
|-------|-----|
| "password authentication failed" | Check clinicos_user password |
| "database does not exist" | Create: `CREATE DATABASE clinicos_db;` |
| "relation does not exist" | Run scripts in order: 1→2→3→4 |
| "permission denied" | Run as postgres user first |
| bash not found on Windows | Use WSL or Git Bash |

---

## ✨ What's Ready After Setup

✅ User registration with approval status
✅ Demo booking form storage
✅ User approval workflow
✅ Dashboard redirect logic (backend)
✅ Admin approval endpoints (backend)
✅ Fast query performance

Still need: Frontend implementation

---

## 🎯 Next Steps

1. ✅ Run these 4 database scripts
2. ⏳ Start backend services
3. ⏳ Test API endpoints
4. ⏳ Build frontend components
5. ⏳ Test full flow
6. ⏳ Deploy

---

**All 4 scripts must run successfully for demo booking to work!**

See `DATABASE_SETUP_GUIDE.md` for complete details.

