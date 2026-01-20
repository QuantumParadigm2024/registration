package com.planotech.plano.helper;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class UploadFileData {
    private String originalName;
    private byte[] data;

    public UploadFileData(String originalName, byte[] data) {
        this.originalName = originalName;
        this.data = data;
    }

    public String getOriginalName() { return originalName; }
    public byte[] getData() { return data; }
}