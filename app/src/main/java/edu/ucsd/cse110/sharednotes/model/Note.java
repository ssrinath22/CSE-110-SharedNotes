package edu.ucsd.cse110.sharednotes.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import org.json.JSONObject;

import java.time.Instant;

@Entity(tableName = "notes")
public class Note {
    /** The title of the note. Used as the primary key for shared notes (even on the cloud). */
    @PrimaryKey
    @SerializedName("title")
    @NonNull
    public String title;

    /** The content of the note. */
    @SerializedName("content")
    @NonNull
    public String content;

    /**
     * When the note was last modified. Used for resolving local (db) vs remote (api) conflicts.
     * Defaults to 0 (Jan 1, 1970), so that if a note already exists remotely, its content is
     * always preferred to a new empty note.
     */
    @SerializedName(value = "version")
    public long version = 0;

    /** General constructor for a note. */
    public Note(@NonNull String title, @NonNull String content) {
        this.title = title;
        this.content = content;
        this.version = 0;
    }

    @Ignore
    public Note(@NonNull String title, @NonNull String content, long version) {
        this.title = title;
        this.content = content;
        this.version = version;
    }

    public static Note fromJSON(String json) {
        return new Gson().fromJson(json, Note.class);
    }

    public String toJSON() {
        return new Gson().toJson(this);
    }

    public String toPutJson(String[] fieldsToInclude){
        ExclusionStrategy strategy = new ExclusionStrategy() {
            @Override
            public boolean shouldSkipField(FieldAttributes attr) {
                for (var field : fieldsToInclude) {
                    if (attr.getName().equals(field)){
                        return false;
                    }
                }
                return true;
            }

            @Override
            public boolean shouldSkipClass(Class<?> clazz) {
                return false;
            }
        };

        Gson gson = new GsonBuilder().addSerializationExclusionStrategy(strategy).create();

        return gson.toJson(this);
    }
}
