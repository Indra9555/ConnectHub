package com.example.connecthub.helpers;

import androidx.annotation.NonNull;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Membership changes for groups. Every change is a Firestore TRANSACTION, so two admins
 * adding / removing people at the same moment can no longer overwrite each other
 * (the old code read the whole group, edited it locally and wrote it back).
 */
public class GroupHelper {

    private GroupHelper() {
    }

    public interface Callback {
        void onSuccess();

        void onFailure(@NonNull Exception e);
    }

    private static DocumentReference ref(String groupId) {
        return FirebaseFirestore.getInstance().collection("Groups").document(groupId);
    }

    @SuppressWarnings("unchecked")
    private static List<String> stringList(DocumentSnapshot snapshot, String field) {
        Object value = snapshot.get(field);
        return value instanceof List ? new ArrayList<>((List<String>) value) : new ArrayList<>();
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> history(DocumentSnapshot snapshot, String uid) {

        Object value = snapshot.get(FieldPath.of("memberHistory", uid));

        List<Map<String, Object>> result = new ArrayList<>();

        if (value instanceof List) {
            for (Object item : (List<Object>) value) {
                if (item instanceof Map) {
                    result.add(new HashMap<>((Map<String, Object>) item));
                }
            }
        }

        return result;
    }

    private static void run(
            com.google.firebase.firestore.Transaction.Function<Void> function,
            Callback callback
    ) {
        FirebaseFirestore.getInstance()
                .runTransaction(function)
                .addOnSuccessListener(unused -> {
                    if (callback != null) callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onFailure(e);
                });
    }

    // ---------------------------------------------------------------------

    public static void addMember(String groupId, String uid, Callback callback) {

        DocumentReference groupRef = ref(groupId);

        run(transaction -> {

            DocumentSnapshot snapshot = transaction.get(groupRef);

            if (!snapshot.exists()) {
                throw new FirebaseFirestoreException("Group not found",
                        FirebaseFirestoreException.Code.NOT_FOUND);
            }

            List<String> members = stringList(snapshot, "members");

            if (members.contains(uid)) {
                return null; // already a member
            }

            members.add(uid);

            long now = System.currentTimeMillis();

            Map<String, Object> info = new HashMap<>();
            info.put("joinedAt", now);
            info.put("leftAt", null);
            info.put("active", true);

            List<Map<String, Object>> periods = history(snapshot, uid);

            Map<String, Object> period = new HashMap<>();
            period.put("joinedAt", now);
            period.put("leftAt", null);
            periods.add(period);

            Map<String, Object> updates = new HashMap<>();
            updates.put("members", members);
            updates.put("membersCount", members.size());
            updates.put("memberInfo." + uid, info);
            updates.put("memberHistory." + uid, periods);

            transaction.update(groupRef, updates);

            return null;

        }, callback);
    }

    /** Used both for "remove member" (by an admin) and "leave group" (by the member). */
    public static void removeMember(String groupId, String uid, Callback callback) {

        DocumentReference groupRef = ref(groupId);

        run(transaction -> {

            DocumentSnapshot snapshot = transaction.get(groupRef);

            if (!snapshot.exists()) {
                return null;
            }

            List<String> members = stringList(snapshot, "members");
            List<String> admins = stringList(snapshot, "admins");
            String createdBy = snapshot.getString("createdBy");

            if (!members.remove(uid)) {
                return null; // not a member any more
            }

            // Nobody left -> the group is deleted
            if (members.isEmpty()) {
                transaction.delete(groupRef);
                return null;
            }

            admins.remove(uid);

            // there must always be at least one admin
            if (admins.isEmpty()) {
                admins.add(members.get(0));
            }

            long now = System.currentTimeMillis();

            List<Map<String, Object>> periods = history(snapshot, uid);

            for (int i = periods.size() - 1; i >= 0; i--) {
                if (periods.get(i).get("leftAt") == null) {
                    periods.get(i).put("leftAt", now);
                    break;
                }
            }

            Map<String, Object> updates = new HashMap<>();
            updates.put("members", members);
            updates.put("admins", admins);
            updates.put("membersCount", members.size());
            updates.put("memberInfo." + uid + ".active", false);
            updates.put("memberInfo." + uid + ".leftAt", now);
            updates.put("memberHistory." + uid, periods);

            // ownership moves to the first remaining admin
            if (uid.equals(createdBy)) {
                updates.put("createdBy", admins.get(0));
            }

            transaction.update(groupRef, updates);

            return null;

        }, callback);
    }

    public static void setAdmin(String groupId, String uid, boolean admin, Callback callback) {

        DocumentReference groupRef = ref(groupId);

        run(transaction -> {

            DocumentSnapshot snapshot = transaction.get(groupRef);

            if (!snapshot.exists()) {
                return null;
            }

            List<String> admins = stringList(snapshot, "admins");

            if (admin) {
                if (!admins.contains(uid)) admins.add(uid);
            } else {
                admins.remove(uid);
                if (admins.isEmpty()) {
                    throw new FirebaseFirestoreException(
                            "A group needs at least one admin",
                            FirebaseFirestoreException.Code.FAILED_PRECONDITION);
                }
            }

            transaction.update(groupRef, "admins", admins);

            return null;

        }, callback);
    }
}
