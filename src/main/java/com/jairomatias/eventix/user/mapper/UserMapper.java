package com.jairomatias.eventix.user.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.jairomatias.eventix.user.dto.UserDetailsView;
import com.jairomatias.eventix.user.dto.UserListItem;
import com.jairomatias.eventix.user.dto.UserUpdateForm;
import com.jairomatias.eventix.user.entity.User;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "roleName", source = "role.name")
    UserListItem toListItem(User user);

    @Mapping(target = "roleName", source = "role.name")
    UserDetailsView toDetailsView(User user);

    @Mapping(target = "roleName", source = "role.name")
    UserUpdateForm toUpdateForm(User user);
}
