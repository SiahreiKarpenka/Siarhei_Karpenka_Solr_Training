package com.wolterskluwer.service.content.validation;

import com.wolterskluwer.service.mime.MimeType;


/**
 * Created to decouple from OSA's class ContentObject
 */
public class InputContentObject {

    private String id;

    private MimeType mimeType;

    private String data;

    public InputContentObject(String id, MimeType mimeType, String data) {
        this.id = id;
        this.mimeType = mimeType;
        this.data = data;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public MimeType getMimeType() {
        return mimeType;
    }

    public void setMimeType(MimeType mimeType) {
        this.mimeType = mimeType;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
