package com.hicct3.projectfinder.service;

import com.hicct3.projectfinder.dto.stack.StackInfoResponseDTO;
import com.hicct3.projectfinder.dto.user.*;
import com.hicct3.projectfinder.global.ErrorCode;
import com.hicct3.projectfinder.global.GeneralException;
import com.hicct3.projectfinder.repository.StackRepository;
import com.hicct3.projectfinder.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final StackRepository stackRepository;

    @Transactional
    public UserProfileResponseDTO getUserProfile(Long userId) {
        var user = userRepository.findById(userId).orElseThrow(()->new GeneralException(ErrorCode.USER_NOT_FOUND));

        return UserProfileResponseDTO.builder()
                .name(user.getNickName())
                .field(user.getField())
                .bio(user.getBio())
                .profileUrl(user.getProfileUrl())
                .verified(user.getVerified())
                .stackInfoList(user.getStacks().stream().
                        map(x->StackInfoResponseDTO.builder()
                                .id(x.getId())
                                .name(x.getName())
                                .build()
                        ).toList())
                .build();
    }

    @Transactional
    public MyProfileResponseDTO getMyProfile(Long userId) {
        var user = userRepository.findById(userId).orElseThrow(()->new GeneralException(ErrorCode.USER_NOT_FOUND));

        return MyProfileResponseDTO.builder()
                .name(user.getNickName())
                .email(user.getEmail())
                .field(user.getField())
                .bio(user.getBio())
                .profileUrl(user.getProfileUrl())
                .verified(user.getVerified())
                .stackInfoList(user.getStacks().stream().
                        map(x->StackInfoResponseDTO.builder()
                                .id(x.getId())
                                .name(x.getName())
                                .build()
                        ).toList())
                .build();
    }

    @Transactional
    public UserSchoolProfileResponseDTO getUserSchoolProfile(Long userId) {
        var user = userRepository.findById(userId).orElseThrow(()->new GeneralException(ErrorCode.USER_NOT_FOUND));
        if(user.getVerified() == false)
            throw new GeneralException(ErrorCode.USER_NOT_VERIFIED);


        return UserSchoolProfileResponseDTO.builder()
                .schoolName(user.getSchoolDomain().getName())
                .major(user.getMajor())
                .grade(user.getGrade())
                .build();
    }

    @Transactional
    public void updateProfile(Long userId, UpdateProfileRequestDTO req)
    {
        var user = userRepository.findById(userId).orElseThrow(()->new GeneralException(ErrorCode.USER_NOT_FOUND));

        if(req.getNickName() != null && !req.getNickName().equals(user.getNickName()))
        {
            if(userRepository.existsByNickName(req.getNickName()))
                throw new GeneralException(ErrorCode.DUPLICATE_NICKNAME);

            user.setNickName(req.getNickName());
        }

        if(req.getBio() != null)
            user.setBio(req.getBio());

        if(req.getStackIds() != null) {
            var distinctStackIds = new LinkedHashSet<>(req.getStackIds());
            var stacks = stackRepository.findAllById(distinctStackIds);

            if (stacks.size() != distinctStackIds.size())
                throw new GeneralException(ErrorCode.STACK_NOT_FOUND);

            user.setStacks(stacks);
        }

        if(req.getField() != null)
            user.setField(req.getField());

        if(req.getProfileUrl() != null)
            user.setProfileUrl(req.getProfileUrl());

    }

    @Transactional
    public void updateSchoolProfile(Long userId, UpdateSchoolProfileRequestDTO req)
    {
        var user = userRepository.findById(userId).orElseThrow(()->new GeneralException(ErrorCode.USER_NOT_FOUND));

        if(!user.getVerified())
            throw new GeneralException(ErrorCode.USER_NOT_VERIFIED);

        if(req.getMajor() != null && !req.getMajor().equals(user.getMajor()))
        {
            user.setMajor(req.getMajor());
        }

        if(req.getGrade() != null && !req.getGrade().equals(user.getGrade()))
        {
            user.setGrade(req.getGrade());
        }

    }
}