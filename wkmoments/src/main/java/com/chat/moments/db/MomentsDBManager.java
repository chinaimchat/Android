package com.chat.moments.db;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;
import android.util.Log;

import com.chat.base.WKBaseApplication;
import com.chat.base.db.WKCursor;

import java.util.ArrayList;
import java.util.List;

/**
 * 2020-11-26 14:51
 * 朋友圈数据库管理
 */
public class MomentsDBManager {
    private MomentsDBManager() {
    }

    private static class MomentsDBManagerBinder {
        final static MomentsDBManager dbManager = new MomentsDBManager();
    }

    public static MomentsDBManager getInstance() {
        return MomentsDBManagerBinder.dbManager;
    }

    /**
     * 与 assets/moments_sql 中表结构一致；迁移未执行时避免查询崩溃。
     */
    private synchronized void ensureSchema() {
        try {
            if (WKBaseApplication.getInstance().getDbHelper() == null) {
                return;
            }
            SQLiteDatabase db = WKBaseApplication.getInstance().getDbHelper().getDB();
            if (db == null) {
                return;
            }
            db.execSQL(
                    "CREATE TABLE IF NOT EXISTS moment_msg("
                            + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                            + "action varchar(40) NOT NULL default '',"
                            + "action_at INTEGER NOT NULL default 0,"
                            + "moment_no varchar(40) NOT NULL default '',"
                            + "content text NOT NULL default '',"
                            + "uid varchar(40) NOT NULL default '',"
                            + "name varchar(100) NOT NULL default '',"
                            + "comment text NOT NULL default '',"
                            + "version bigint NOT NULL default 0,"
                            + "is_deleted int NOT NULL default 0,"
                            + "created_at text,"
                            + "updated_at text,"
                            + "comment_id INTEGER NOT NULL default 0"
                            + ")"
            );
        } catch (Exception e) {
            Log.e("MomentsDBManager", "ensureSchema", e);
        }
    }

    //添加动态消息
    public void insert(MomentsDBMsg mdbMomentsMsg) {
        ensureSchema();
        if (mdbMomentsMsg.is_deleted == 1) {
            if (mdbMomentsMsg.comment_id != 0) {
                Log.e("修改消息","-->");
                ContentValues contentValues = new ContentValues();
                contentValues.put("is_deleted", 1);
                String where = "comment_id=? and moment_no=?";
                String[] whereValue = new String[2];
                whereValue[0] = mdbMomentsMsg.comment_id + "";
                whereValue[1] = mdbMomentsMsg.moment_no + "";
                WKBaseApplication.getInstance().getDbHelper()
                        .update("moment_msg", contentValues, where, whereValue);
            }
        } else {
            Log.e("新增消息","-->");
            ContentValues contentValues = getMsgContentValues(mdbMomentsMsg);
            WKBaseApplication.getInstance().getDbHelper().insert("moment_msg", contentValues);
        }

    }

    //删除动态
    public boolean delete(int id) {
        ensureSchema();
        String[] where = new String[]{String.valueOf(id)};
        ContentValues contentValues = new ContentValues();
        contentValues.put("is_deleted", 1);
        return WKBaseApplication.getInstance().getDbHelper().update("moment_msg", contentValues, "id=?", where);
    }

    //清空所有动态消息
    public boolean clear() {
        ensureSchema();
        ContentValues contentValues = new ContentValues();
        contentValues.put("is_deleted", 1);
        return WKBaseApplication.getInstance().getDbHelper().update("moment_msg", contentValues, "", null);
    }

    public List<MomentsDBMsg> query() {
        ensureSchema();
        List<MomentsDBMsg> list = new ArrayList<>();
        Cursor cursor = WKBaseApplication
                .getInstance()
                .getDbHelper()
                .rawQuery(
                        "select * from moment_msg order by action_at desc", null);
        if (cursor == null) {
            return list;
        }
        for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
            list.add(serializeMomentMsg(cursor));
        }
        cursor.close();
        Log.e("查询点的数量", list.size() + "");
        return list;
    }

    private ContentValues getMsgContentValues(MomentsDBMsg mdbMomentsMsg) {
        ContentValues contentValues = new ContentValues();
        contentValues.put("action", TextUtils.isEmpty(mdbMomentsMsg.action) ? "" : mdbMomentsMsg.action);
        contentValues.put("action_at", mdbMomentsMsg.action_at);
        contentValues.put("moment_no", TextUtils.isEmpty(mdbMomentsMsg.moment_no) ? "" : mdbMomentsMsg.moment_no);
        contentValues.put("content", TextUtils.isEmpty(mdbMomentsMsg.content) ? "" : mdbMomentsMsg.content);
        contentValues.put("uid", TextUtils.isEmpty(mdbMomentsMsg.uid) ? "" : mdbMomentsMsg.uid);
        contentValues.put("name", TextUtils.isEmpty(mdbMomentsMsg.name) ? "" : mdbMomentsMsg.name);
        contentValues.put("comment", TextUtils.isEmpty(mdbMomentsMsg.comment) ? "" : mdbMomentsMsg.comment);
        contentValues.put("version", mdbMomentsMsg.version);
        contentValues.put("is_deleted", mdbMomentsMsg.is_deleted);
        contentValues.put("comment_id", mdbMomentsMsg.comment_id);
        contentValues.put("created_at", TextUtils.isEmpty(mdbMomentsMsg.created_at) ? "" : mdbMomentsMsg.created_at);
        contentValues.put("updated_at", TextUtils.isEmpty(mdbMomentsMsg.updated_at) ? "" : mdbMomentsMsg.updated_at);
        return contentValues;
    }

    private MomentsDBMsg serializeMomentMsg(Cursor cursor) {
        MomentsDBMsg dbMsg = new MomentsDBMsg();
        dbMsg.id = WKCursor.readInt(cursor, "id");
        dbMsg.action = WKCursor.readString(cursor, "action");
        dbMsg.moment_no = WKCursor.readString(cursor, "moment_no");
        dbMsg.content = WKCursor.readString(cursor, "content");
        dbMsg.uid = WKCursor.readString(cursor, "uid");
        dbMsg.name = WKCursor.readString(cursor, "name");
        dbMsg.comment = WKCursor.readString(cursor, "comment");
        dbMsg.version = WKCursor.readLong(cursor, "version");
        dbMsg.comment_id = WKCursor.readInt(cursor, "comment_id");
        dbMsg.is_deleted = WKCursor.readInt(cursor, "is_deleted");
        dbMsg.created_at = WKCursor.readString(cursor, "created_at");
        dbMsg.updated_at = WKCursor.readString(cursor, "updated_at");
        dbMsg.action_at = WKCursor.readLong(cursor, "action_at");
        return dbMsg;
    }

}
