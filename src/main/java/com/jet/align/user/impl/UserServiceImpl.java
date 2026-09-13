package com.jet.align.user.impl;

import com.jet.align.common.exception.BusinessException;
import com.jet.align.common.exception.ResourceNotFoundException;
import com.jet.align.user.User;
import com.jet.align.user.UserAvatar;
import com.jet.align.user.UserAvatarRepository;
import com.jet.align.user.UserMapper;
import com.jet.align.user.UserRepository;
import com.jet.align.user.UserService;
import com.jet.align.user.dto.AvatarContent;
import com.jet.align.user.dto.EmailUpdateRequest;
import com.jet.align.user.dto.PasswordUpdateRequest;
import com.jet.align.user.dto.ProfileUpdateRequest;
import com.jet.align.user.dto.UserResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository repository;
    private final UserAvatarRepository avatarRepository;
    private final UserMapper mapper;
    private final PasswordEncoder passwordEncoder;

    private static final String USER_NOT_FOUND_MESSAGE = "No se encontró el usuario con id: ";
    private static final String WRONG_CURRENT_PASSWORD_MESSAGE = "La contraseña actual es incorrecta.";
    private static final String EMAIL_ALREADY_REGISTERED_MESSAGE = "El correo electrónico ya está registrado.";
    private static final String AVATAR_NOT_FOUND_MESSAGE = "El usuario no tiene foto de perfil.";
    private static final String AVATAR_EMPTY_MESSAGE = "La foto de perfil está vacía.";
    private static final String AVATAR_TYPE_MESSAGE = "La foto de perfil debe ser JPEG, PNG o WebP.";
    private static final String AVATAR_SIZE_MESSAGE = "La foto de perfil no puede superar los 512 KB.";

    static final int MAX_AVATAR_BYTES = 512 * 1024;
    static final Set<String> ALLOWED_AVATAR_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

    public UserServiceImpl(UserRepository repository,
                           UserAvatarRepository avatarRepository,
                           UserMapper mapper,
                           PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.avatarRepository = avatarRepository;
        this.mapper = mapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserResponse getProfile(User user) {
        // El principal ya fue cargado desde la DB por el filtro JWT en este mismo
        // request, así que no se recarga; la única query es la de hasAvatar.
        return toResponse(user);
    }

    @Override
    @Transactional
    public UserResponse updateProfile(User user, ProfileUpdateRequest request) {
        // Para escribir sí se recarga: el principal es una entidad detached (cargada
        // fuera de esta transacción). Se muta la instancia gestionada, como hacen
        // Task/Habit con findByIdAndUser antes de actualizar.
        User managed = findManaged(user);
        mapper.updateEntity(request, managed);
        return toResponse(repository.save(managed));
    }

    @Override
    @Transactional
    public void changePassword(User user, PasswordUpdateRequest request) {
        User managed = findManaged(user);
        verifyCurrentPassword(request.currentPassword(), managed);
        managed.setPassword(passwordEncoder.encode(request.newPassword()));
        repository.save(managed);
    }

    @Override
    @Transactional
    public UserResponse changeEmail(User user, EmailUpdateRequest request) {
        User managed = findManaged(user);
        verifyCurrentPassword(request.currentPassword(), managed);

        // Cambiar al mismo correo es un no-op, no un "ya está registrado" contra uno mismo.
        if (request.newEmail().equals(managed.getEmail())) {
            return toResponse(managed);
        }
        if (repository.existsByEmail(request.newEmail())) {
            throw new BusinessException(EMAIL_ALREADY_REGISTERED_MESSAGE);
        }
        // No hay que reemitir el token: el subject del JWT es el id, no el email.
        managed.setEmail(request.newEmail());
        return toResponse(repository.save(managed));
    }

    @Override
    @Transactional
    public void saveAvatar(User user, AvatarContent avatar) {
        validateAvatar(avatar);
        // Upsert: subir de nuevo reemplaza la foto, no choca contra uk_user_avatars_user.
        UserAvatar entity = avatarRepository.findByUser(user).orElseGet(() -> {
            UserAvatar created = new UserAvatar();
            created.setUser(user);
            return created;
        });
        entity.setContentType(avatar.contentType());
        entity.setData(avatar.data());
        avatarRepository.save(entity);
    }

    @Override
    public AvatarContent getAvatar(User user) {
        return avatarRepository.findByUser(user)
                .map(avatar -> new AvatarContent(avatar.getContentType(), avatar.getData()))
                .orElseThrow(() -> new ResourceNotFoundException(AVATAR_NOT_FOUND_MESSAGE));
    }

    @Override
    @Transactional
    public void deleteAvatar(User user) {
        // Idempotente: sin foto es un no-op, igual que unsubscribe en notification.
        avatarRepository.deleteByUser(user);
    }

    private void validateAvatar(AvatarContent avatar) {
        if (avatar.data() == null || avatar.data().length == 0) {
            throw new BusinessException(AVATAR_EMPTY_MESSAGE);
        }
        if (avatar.contentType() == null || !ALLOWED_AVATAR_TYPES.contains(avatar.contentType())) {
            throw new BusinessException(AVATAR_TYPE_MESSAGE);
        }
        if (avatar.data().length > MAX_AVATAR_BYTES) {
            throw new BusinessException(AVATAR_SIZE_MESSAGE);
        }
    }

    private UserResponse toResponse(User user) {
        return mapper.toResponse(user, avatarRepository.existsByUser(user));
    }

    private void verifyCurrentPassword(String rawPassword, User managed) {
        if (!passwordEncoder.matches(rawPassword, managed.getPassword())) {
            throw new BusinessException(WRONG_CURRENT_PASSWORD_MESSAGE);
        }
    }

    private User findManaged(User user) {
        return repository.findById(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND_MESSAGE + user.getId()));
    }

}
