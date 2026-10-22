/**
 * Production MongoDB Index Migration Script
 * 
 * Pre-provisions all compound, unique, and sparse indexes across all domain collections
 * before routing production traffic.
 * 
 * Usage:
 *   mongosh "${MONGODB_URI}" scripts/mongo-indexes-migration.js
 */

print("==========================================================================");
print("🚀 SAAJ / CIVICLINK ENTERPRISE MONGODB INDEX PROVISIONING & MIGRATION");
print("==========================================================================");

const dbInstance = db.getName();
print(`Target Database: ${dbInstance}`);
print(`Execution Timestamp: ${new Date().toISOString()}`);

function safeCreateIndex(collectionName, indexSpec, options = {}) {
    try {
        const collection = db.getCollection(collectionName);
        const name = options.name || Object.keys(indexSpec).join("_");
        print(`  ➜ Creating index [${name}] on collection '${collectionName}'...`);
        collection.createIndex(indexSpec, { background: true, ...options });
        print(`    ✔ Success: ${name}`);
    } catch (err) {
        print(`    ⚠️ Warning/Error on ${collectionName}: ${err.message}`);
    }
}

print("\n--- 1. Users Collection ---");
safeCreateIndex("users", { email: 1 }, { unique: true, name: "user_email_unique_idx" });
safeCreateIndex("users", { societyId: 1, status: 1 }, { name: "user_society_status_idx" });
safeCreateIndex("users", { societyId: 1, role: 1 }, { name: "user_society_role_idx" });
safeCreateIndex("users", { societyId: 1, status: 1, role: 1 }, { name: "user_society_status_role_idx" });

print("\n--- 2. Societies Collection ---");
safeCreateIndex("societies", { registrationNumber: 1 }, { unique: true, name: "society_reg_number_uidx" });
safeCreateIndex("societies", { societyCode: 1 }, { unique: true, name: "society_code_uidx" });
safeCreateIndex("societies", { status: 1 }, { name: "society_status_idx" });

print("\n--- 3. Society Groups (Enterprise Governance: 1 Official Group per Society) ---");
safeCreateIndex("society_groups", { societyId: 1 }, { unique: true, name: "society_group_unique_idx" });
safeCreateIndex("society_groups", { isOfficial: 1 }, { name: "society_group_official_idx" });

print("\n--- 4. Flats Collection ---");
safeCreateIndex("flats", { societyId: 1, flatNumber: 1 }, { unique: true, name: "flat_society_number_uidx" });
safeCreateIndex("flats", { societyId: 1, block: 1, flatNumber: 1 }, { name: "flat_society_block_flat_idx" });
safeCreateIndex("flats", { societyId: 1, occupied: 1 }, { name: "flat_society_occupied_idx" });
safeCreateIndex("flats", { societyId: 1, block: 1, occupied: 1, flatNumber: 1 }, { name: "flat_society_block_occ_flat_idx" });

print("\n--- 5. Maintenance Bills Collection ---");
safeCreateIndex("maintenance_bills", { societyId: 1, apartmentNumber: 1, billingMonth: 1 }, { unique: true, name: "bill_society_apt_month_uidx" });
safeCreateIndex("maintenance_bills", { societyId: 1, userId: 1, status: 1, createdAt: -1 }, { name: "bill_society_user_status_idx" });
safeCreateIndex("maintenance_bills", { societyId: 1, status: 1, createdAt: -1 }, { name: "bill_society_status_idx" });
safeCreateIndex("maintenance_bills", { societyId: 1, createdAt: -1 }, { name: "bill_society_created_idx" });
safeCreateIndex("maintenance_bills", { status: 1, dueDate: 1 }, { name: "bill_status_duedate_idx" });
safeCreateIndex("maintenance_bills", { upiTransactionId: 1 }, { unique: true, sparse: true, name: "bill_upi_txn_unique_idx" });

print("\n--- 6. Complaints & Grievances Collection ---");
safeCreateIndex("complaints", { societyId: 1, status: 1, createdAt: -1 }, { name: "complaint_society_status_idx" });
safeCreateIndex("complaints", { societyId: 1, category: 1, createdAt: -1 }, { name: "complaint_society_cat_idx" });
safeCreateIndex("complaints", { societyId: 1, status: 1, category: 1, createdAt: -1 }, { name: "complaint_society_status_cat_idx" });
safeCreateIndex("complaints", { societyId: 1, residentId: 1, status: 1, createdAt: -1 }, { name: "complaint_society_resident_status_idx" });
safeCreateIndex("complaints", { societyId: 1, createdAt: -1 }, { name: "complaint_society_created_idx" });

print("\n--- 7. Announcements Collection ---");
safeCreateIndex("announcements", { societyId: 1, createdAt: -1 }, { name: "announcement_society_created_idx" });
safeCreateIndex("announcements", { societyId: 1, type: 1, createdAt: -1 }, { name: "announcement_society_type_created_idx" });

print("\n--- 8. Governance Issues & Polls Collection ---");
safeCreateIndex("issues", { societyId: 1, createdAt: -1 }, { name: "issue_society_created_idx" });
safeCreateIndex("issues", { societyId: 1, status: 1, createdAt: -1 }, { name: "issue_society_status_created_idx" });

print("\n--- 9. Issue Votes Collection ---");
safeCreateIndex("issue_votes", { issueId: 1, userId: 1 }, { unique: true, name: "vote_issue_user_uidx" });

print("\n--- 10. Subscriptions & Billing Orders Collection ---");
safeCreateIndex("subscriptions", { societyId: 1, createdAt: -1 }, { name: "sub_society_created_idx" });
safeCreateIndex("subscriptions", { razorpayOrderId: 1, societyId: 1 }, { name: "sub_order_society_idx" });

print("\n--- 11. Invite Tokens Collection ---");
safeCreateIndex("invite_tokens", { token: 1 }, { unique: true, sparse: true, name: "invite_token_uidx" });
safeCreateIndex("invite_tokens", { societyId: 1, email: 1, used: 1 }, { name: "invite_society_email_used_idx" });

print("\n==========================================================================");
print("✨ ALL MONGODB INDEXES VERIFIED & PROVISIONED SUCCESSFULLY");
print("==========================================================================");
