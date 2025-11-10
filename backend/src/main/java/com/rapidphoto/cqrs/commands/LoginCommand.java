package com.rapidphoto.cqrs.commands;

/**
 * Command to authenticate a user.
 */
public record LoginCommand(
    String email,
    String password
) {}
