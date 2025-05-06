package DTO;

import java.io.Serializable;

public enum CommandType implements Serializable {
    REGISTER,
    LOGIN,
    UPLOAD,
    DOWNLOAD,
    CREATE_FOLDER,
    DELETE,
    SEARCH,
    COPY,
    RENAME,
    SORT,
    MOVE,
    LIST_FILES
}   