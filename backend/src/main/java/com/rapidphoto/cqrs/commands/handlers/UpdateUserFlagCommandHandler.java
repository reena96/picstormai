package com.rapidphoto.cqrs.commands.handlers;

import com.rapidphoto.cqrs.commands.UpdateUserFlagCommand;
import com.rapidphoto.domain.user.User;
import com.rapidphoto.domain.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

/**
 * Handler for UpdateUserFlagCommand.
 * Updates user flags such as hasSeenOnboarding.
 */
@Service
public class UpdateUserFlagCommandHandler {

    private final UserRepository userRepository;

    public UpdateUserFlagCommandHandler(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Handle the command to mark onboarding as complete.
     * @param command Command containing userId
     * @return Mono of updated User
     */
    @Transactional(rollbackFor = Exception.class)
    public Mono<User> handle(UpdateUserFlagCommand command) {
        return userRepository.findById(command.userId())
            .switchIfEmpty(Mono.error(new IllegalArgumentException("User not found with ID: " + command.userId())))
            .flatMap(user -> {
                user.markOnboardingComplete();
                return userRepository.save(user);
            });
    }
}
