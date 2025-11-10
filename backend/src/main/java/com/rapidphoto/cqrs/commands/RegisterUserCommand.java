package com.rapidphoto.cqrs.commands;

/**
 * Command to register a new user.
 */
public record RegisterUserCommand(
    String email,
    String password,
    String displayName
) {}
