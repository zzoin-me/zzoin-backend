ALTER TABLE users
    MODIFY COLUMN profile_url VARCHAR(2048) NULL;

ALTER TABLE users
    ADD COLUMN social_profile_url VARCHAR(2048) NULL;

UPDATE users
SET social_profile_url = profile_url
WHERE (provider = 'google' AND profile_url LIKE 'https://lh3.googleusercontent.com/%')
   OR (provider = 'kakao' AND profile_url LIKE '%kakaocdn.net/%');
