package com.hicct3.projectfinder.service;

import com.hicct3.projectfinder.dto.auth.*;
import com.hicct3.projectfinder.entity.EmailVerification;
import com.hicct3.projectfinder.entity.RefreshToken;
import com.hicct3.projectfinder.entity.User;
import com.hicct3.projectfinder.entity.enums.VerificationType;
import com.hicct3.projectfinder.global.CustomUserDetails;
import com.hicct3.projectfinder.global.ErrorCode;
import com.hicct3.projectfinder.global.GeneralException;
import com.hicct3.projectfinder.global.JwtProvider;
import com.hicct3.projectfinder.global.NicknamePolicy;
import com.hicct3.projectfinder.repository.SchoolDomainRepository;
import com.hicct3.projectfinder.repository.EmailVerificationRepository;
import com.hicct3.projectfinder.repository.RefreshTokenRepository;
import com.hicct3.projectfinder.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.security.SecureRandom;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthService {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final SchoolDomainRepository schoolDomainRepository;
    private final EmailVerificationRepository emailVerificationRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProvider jwtProvider;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;
    private final AuthenticationManager authenticationManager;
    private final EmailVerificationAttemptService emailVerificationAttemptService;
    private final AccountLifecycleService accountLifecycleService;

    @Value("${spring.mail.username}")
    private String fromEmail;

    //로그인
    @Transactional
    public LoginResponseDTO login(LoginRequestDTO req) {
        //인증 완료시 auth 생성
        try
        {
            var lowerEmail = req.getEmail().trim().toLowerCase();
            var auth = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(lowerEmail, req.getPassword()));

            var user = ((CustomUserDetails) auth.getPrincipal()).getUser();

            if(user.isDeleted()) {
                if (!accountLifecycleService.isRecoverable(user)) {
                    accountLifecycleService.finalizeExpiredAccount(user.getUserId());
                    throw new GeneralException(ErrorCode.ACCOUNT_RECOVERY_EXPIRED);
                }
                return recoveryResponse(user);
            }

            return issueTokens(user);
        } catch(AuthenticationException e)
        {
            throw new GeneralException(ErrorCode.AUTHENTICATION_FAILED);
        }

    }

    //로그아웃
    @Transactional
    public void logout(Long userId) {
        refreshTokenRepository.deleteByUserId(userId);
    }

    //회원탈퇴
    @Transactional
    public void withdraw(Long userId, WithDrawEmailVerifyRequestDTO req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorCode.USER_NOT_FOUND));

        if (Boolean.TRUE.equals(user.isDeleted())) {
            throw new GeneralException(ErrorCode.USER_NOT_FOUND);
        }

        emailVerificationAttemptService.verifyAndConsume(
                user.getEmail(), VerificationType.WITHDRAW, req.getCode(), userId);

        String originalEmail = user.getEmail();

        refreshTokenRepository.deleteByUserId(userId);
        user.requestWithdrawal(LocalDateTime.now());

        sendWithdrawCompletedEmail(originalEmail);
    }

    //회원 탈퇴 이메일 전송
    @Transactional
    public void sendWithDrawEmail(Long userId)
    {
        var user = userRepository.findById(userId).orElseThrow(()->new GeneralException(ErrorCode.USER_NOT_FOUND));
        if (Boolean.TRUE.equals(user.isDeleted())) {
            throw new GeneralException(ErrorCode.USER_NOT_FOUND);
        }

        String code = createCode();

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(user.getEmail());
        message.setSubject("회원탈퇴 확인 인증번호");
        message.setText("회원탈퇴를 진행하려면 아래 인증번호를 입력해주세요.\n"
                + "인증번호는 [" + code + "] 입니다.\n"
                + "인증번호는 5분간 유효합니다.\n\n"
                + "본인이 요청한 것이 아니라면 이 메일을 무시해주세요.");

        String encodedCode = passwordEncoder.encode(code);
        emailVerificationRepository.findByEmailAndType(user.getEmail(), VerificationType.WITHDRAW)
                .ifPresentOrElse(
                        verification -> verification.update(encodedCode, user, LocalDateTime.now().plusMinutes(5)),
                        () -> emailVerificationRepository.save(
                                new EmailVerification(
                                        user.getEmail(),
                                        VerificationType.WITHDRAW,
                                        encodedCode,
                                        user,
                                        LocalDateTime.now().plusMinutes(5)
                                )
                        )
                );

        try {
            mailSender.send(message);
        } catch (MailException e) {
            throw new GeneralException(ErrorCode.EMAIL_SEND_FAILED);
        }
    }


    //회원가입
    @Transactional
    public void signUp(SignUpRequestDTO req)
    {
        var lowerEmail = req.getEmail().trim().toLowerCase();
        var tokenEmail = jwtProvider.verifySignupTokenAndGetEmail(req.getSignupToken()).trim().toLowerCase();
        var normalizedNickname = NicknamePolicy.normalizeAndValidate(req.getNickName());

        if (!lowerEmail.equals(tokenEmail)) {
            throw new GeneralException(ErrorCode.SIGNUP_EMAIL_MISMATCH);
        }

        if(userRepository.existsByNickName(normalizedNickname))
        {
            throw new GeneralException(ErrorCode.DUPLICATE_NICKNAME);
        }

        //이미 가입되었는지 조회
        assertEmailAvailableForSignup(lowerEmail);

        if(userRepository.existsByVerifiedEmail(lowerEmail))
        {
            throw new GeneralException(ErrorCode.DUPLICATE_VERIFIED_EMAIL);
        }

        String domain = getDomain(lowerEmail);
        var schoolDomainOpt = schoolDomainRepository.findByMatchingDomain(domain);

        var user = User.builder()
                .nickName(normalizedNickname)
                .email(lowerEmail)
                .password(passwordEncoder.encode(req.getPassword()))
                .verified(schoolDomainOpt.isPresent())
                .admin(false)
                .localLoginEnabled(true)
                .nicknameChangedAt(java.time.LocalDateTime.now())
                .build();

        if (schoolDomainOpt.isPresent()) {
            user.setVerifiedEmail(lowerEmail);
            user.setSchoolDomain(schoolDomainOpt.get());
        }

        userRepository.save(user);

    }

    //회원가입 이메일 코드 전송
    @Transactional
    public void sendSignupEmail(String email)
    {
        var lowerEmail = email.trim().toLowerCase();

        assertEmailAvailableForSignup(lowerEmail);

        String code = createCode();

        SimpleMailMessage message = new SimpleMailMessage();

        message.setFrom(fromEmail);
        message.setTo(lowerEmail);
        message.setSubject("회원가입 인증번호");
        message.setText("인증번호는 [" + code + "] 입니다.\n인증번호는 5분간 유효합니다.");

        String encodedCode = passwordEncoder.encode(code);
        emailVerificationRepository.findByEmailAndType(lowerEmail, VerificationType.SIGNUP)
                .ifPresentOrElse(
                        verification -> verification.update(encodedCode, null, LocalDateTime.now().plusMinutes(5)),

                        () -> emailVerificationRepository.save(
                                new EmailVerification(
                                        lowerEmail,
                                        VerificationType.SIGNUP,
                                        encodedCode,
                                        null,
                                        LocalDateTime.now().plusMinutes(5)
                                )
                        )
                );

        try
        {
            mailSender.send(message);
        }
        catch (MailException e)
        {
            throw new GeneralException(ErrorCode.EMAIL_SEND_FAILED);
        }
    }

    //회원가입 이메일 코드 확인
    @Transactional
    public EmailVerifyResponseDTO verifySignupEmail(EmailVerifyRequestDTO req)
    {
        var lowerVerifyEmail = req.getVerifyEmail().trim().toLowerCase();
        emailVerificationAttemptService.verifyAndConsume(
                lowerVerifyEmail, VerificationType.SIGNUP, req.getCode(), null);

        return EmailVerifyResponseDTO.builder()
                .token(jwtProvider.createSignupToken(lowerVerifyEmail))
                .build();
    }

    //토큰 재발급
    @Transactional
    public RefreshTokenResponseDTO refreshToken(String refreshToken)
    {
        // 토큰 검증
        Long userId = jwtProvider.verifyRefreshTokenAndGetUserId(refreshToken);

        RefreshToken savedToken = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new GeneralException("유효하지 않은 토큰입니다."));

        if (!savedToken.getUserId().equals(userId)) {
            throw new GeneralException("토큰 정보가 일치하지 않습니다.");
        }

        var user = userRepository.findById(userId).orElseThrow(()->new GeneralException(ErrorCode.USER_NOT_FOUND));
        if(user.isDeleted())
            throw new GeneralException(ErrorCode.USER_WITHDRAWN);


        var accessToken = jwtProvider.createAccessToken(savedToken.getUserId());
        var newRefreshToken = jwtProvider.createRefreshToken(savedToken.getUserId());

        //리프래시 토큰 업데이트
        refreshTokenRepository.findByUserId(savedToken.getUserId())
                .ifPresentOrElse(
                        tokenEntity -> tokenEntity.update(newRefreshToken),
                        () -> refreshTokenRepository.save(RefreshToken.builder()
                                .userId(savedToken.getUserId())
                                .token(newRefreshToken)
                                .build())
                );

        return RefreshTokenResponseDTO.builder()
                .accessToken(accessToken)
                .refreshToken(newRefreshToken)
                .build();
    }

    @Transactional
    public LoginResponseDTO recoverAccount(String recoveryToken) {
        Long userId = jwtProvider.verifyAccountRecoveryToken(recoveryToken);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorCode.USER_NOT_FOUND));

        if (!accountLifecycleService.isRecoverable(user)) {
            accountLifecycleService.finalizeExpiredAccount(user.getUserId());
            throw new GeneralException(ErrorCode.ACCOUNT_RECOVERY_EXPIRED);
        }

        accountLifecycleService.recover(user);
        return issueTokens(user);
    }

    //대학 이메일 코드 전송
    @Transactional
    public void sendEmail(Long userId, String email)
    {
        var lowerEmail = email.trim().toLowerCase();

        userRepository.findByAnyEmail(lowerEmail).ifPresentOrElse(
                user -> {
                    if (!user.getUserId().equals(userId)) {
                        throw new GeneralException(ErrorCode.EMAIL_USED_BY_OTHER_ACCOUNT);
                    }
                },
                () -> {
                    //pass
                }
        );

        String emailDomain = getDomain(lowerEmail);
        if (!schoolDomainRepository.existsByMatchingDomain(emailDomain))
            throw new GeneralException(ErrorCode.NOT_UNIVERSITY_EMAIL);

        User user = userRepository.findById(userId)
                .orElseThrow(()->new GeneralException(ErrorCode.USER_NOT_FOUND));

        //이메일 전송
        String code = createCode();

        SimpleMailMessage message = new SimpleMailMessage();

        message.setFrom(fromEmail);
        message.setTo(lowerEmail);
        message.setSubject("대학 인증 인증번호");
        message.setText("인증번호는 [" + code + "] 입니다.\n인증번호는 5분간 유효합니다.");


        //인증코드 이미 있으면 업데이트
        String encodedCode = passwordEncoder.encode(code);
        emailVerificationRepository.findByEmailAndType(lowerEmail, VerificationType.UNIVERSITY)
                .ifPresentOrElse(
                        verification -> verification.update(encodedCode, user, LocalDateTime.now().plusMinutes(5)),

                        () -> emailVerificationRepository.save(new EmailVerification(lowerEmail, VerificationType.UNIVERSITY, encodedCode, user, LocalDateTime.now().plusMinutes(5)))
                );

        //메일 전송
        try
        {
            mailSender.send(message);
        }
        catch (MailException e)
        {
            throw new GeneralException(ErrorCode.EMAIL_SEND_FAILED);
        }

    }

    //대학 이메일 코드 확인
    @Transactional
    public void verifyEmail(Long userId, UnivEmailVerifyRequestDTO req)
    {
        var lowerVerifyEmail = req.getVerifyEmail().trim().toLowerCase();
        emailVerificationAttemptService.verifyAndConsume(
                lowerVerifyEmail, VerificationType.UNIVERSITY, req.getCode(), userId);

        var user = userRepository.findById(userId).orElseThrow(()-> new GeneralException(ErrorCode.USER_NOT_FOUND));

        user.setVerified(true);
        user.setVerifiedEmail(lowerVerifyEmail);

        String emailDomain = getDomain(lowerVerifyEmail);
        var schoolDomainOpt = schoolDomainRepository.findByMatchingDomain(emailDomain);
        if (schoolDomainOpt.isEmpty())
            throw new GeneralException(ErrorCode.NOT_UNIVERSITY_EMAIL);
        user.setSchoolDomain(schoolDomainOpt.get());

    }

    private String getDomain(String email) {
        int atIndex = email.indexOf("@");
        if (atIndex == -1 || atIndex == email.length() - 1) {
                throw new GeneralException("올바르지 않은 이메일 형식입니다.");
            }
        return email.substring(atIndex + 1);
    }

    private void assertEmailAvailableForSignup(String email) {
        Optional<User> existing = userRepository.findByAnyEmail(email);
        if (existing.isEmpty()) return;

        User user = existing.get();
        if (!user.isDeleted()) {
            throw new GeneralException(ErrorCode.DUPLICATE_EMAIL);
        }
        if (accountLifecycleService.finalizeIfExpired(user)) {
            return;
        }
        throw new GeneralException(ErrorCode.ACCOUNT_RECOVERY_AVAILABLE);
    }

    private LoginResponseDTO recoveryResponse(User user) {
        return LoginResponseDTO.builder()
                .recoveryRequired(true)
                .recoveryToken(jwtProvider.createAccountRecoveryToken(user.getUserId()))
                .recoverableUntil(user.getRecoverableUntil())
                .build();
    }

    private LoginResponseDTO issueTokens(User user) {
        String accessToken = jwtProvider.createAccessToken(user.getUserId());
        String refreshToken = jwtProvider.createRefreshToken(user.getUserId());
        refreshTokenRepository.findByUserId(user.getUserId())
                .ifPresentOrElse(
                        tokenEntity -> tokenEntity.update(refreshToken),
                        () -> refreshTokenRepository.save(RefreshToken.builder()
                                .userId(user.getUserId())
                                .token(refreshToken)
                                .build())
                );
        return LoginResponseDTO.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .recoveryRequired(false)
                .build();
    }

    private String createCode() {
        StringBuilder code = new StringBuilder();

        for (int i = 0; i < 6; i++) {
            code.append(SECURE_RANDOM.nextInt(10));
        }

        return code.toString();
    }

    private void sendWithdrawCompletedEmail(String email) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(email);
        message.setSubject("회원탈퇴가 접수되었습니다.");
        message.setText("Zzoin 회원탈퇴가 접수되었습니다.\n"
                + "탈퇴일로부터 30일 이내에는 같은 계정으로 로그인하여 복구할 수 있습니다.\n"
                + "30일이 지나면 개인정보가 익명화되며 같은 로그인 수단으로 새 계정을 만들 수 있습니다.");

        try {
            mailSender.send(message);
        } catch (MailException ignored) {

        }
    }
}
