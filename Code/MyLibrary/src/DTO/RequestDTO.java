package DTO;

import java.io.Serializable;

public abstract class RequestDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    private final CommandType command;

    public RequestDTO(CommandType command) {
        this.command = command;
    }

    public CommandType getCommand() {
        return command;
    }
}