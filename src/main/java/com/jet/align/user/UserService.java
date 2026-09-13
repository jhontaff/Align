package com.jet.align.user;

import com.jet.align.user.dto.AvatarContent;
import com.jet.align.user.dto.EmailUpdateRequest;
import com.jet.align.user.dto.PasswordUpdateRequest;
import com.jet.align.user.dto.ProfileUpdateRequest;
import com.jet.align.user.dto.UserResponse;

public interface UserService {

    UserResponse getProfile(User user);

    UserResponse updateProfile(User user, ProfileUpdateRequest request);

    void changePassword(User user, PasswordUpdateRequest request);

    UserResponse changeEmail(User user, EmailUpdateRequest request);

    void saveAvatar(User user, AvatarContent avatar);

    AvatarContent getAvatar(User user);

    void deleteAvatar(User user);
}
