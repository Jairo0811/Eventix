package com.jairomatias.eventix.profile.service;

import com.jairomatias.eventix.profile.dto.ProfileAccountView;
import com.jairomatias.eventix.profile.dto.ProfileUpdateForm;
import com.jairomatias.eventix.profile.dto.ProfileUpdateResult;

public interface ProfileService {

    ProfileAccountView findOwnProfile(String authenticatedLogin);

    ProfileUpdateForm getOwnUpdateForm(String authenticatedLogin);

    ProfileUpdateResult updateOwnProfile(
            String authenticatedLogin,
            ProfileUpdateForm form);
}
