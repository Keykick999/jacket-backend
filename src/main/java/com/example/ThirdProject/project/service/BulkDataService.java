package com.example.ThirdProject.project.service;

import com.amazonaws.services.s3.AmazonS3;
import com.example.ThirdProject.album.entity.Album;
import com.example.ThirdProject.album.repository.AlbumRepository;
import com.example.ThirdProject.project.entity.Clip;
import com.example.ThirdProject.project.entity.Project;
import com.example.ThirdProject.project.entity.Track;
import com.example.ThirdProject.project.enums.TrackType;
import com.example.ThirdProject.project.repository.ClipRepository;
import com.example.ThirdProject.project.repository.ProjectRepository;
import com.example.ThirdProject.project.repository.TrackRepository;
import com.example.ThirdProject.user.entity.User;
import com.example.ThirdProject.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.Date;

@Service
@RequiredArgsConstructor
@Slf4j
public class BulkDataService {

    private final UserRepository userRepository;
    private final AlbumRepository albumRepository;
    private final ProjectRepository projectRepository;
    private final TrackRepository trackRepository;
    private final ClipRepository clipRepository;
    private final AmazonS3 s3Client;

    // 비밀번호 암호화를 위한 인코더 주입
    private final PasswordEncoder passwordEncoder;

    @Value("${cloud.aws.s3.bucket}")
    private String bucketName;

    @Transactional
    public void createBulkTestData() {
        int totalFiles = 400;
        int userCount = 100;
        int tracksPerProject = 15;
        int clipsPerTrack = 5;

        log.info("🚀 부하 테스트용 데이터 생성 시작 (암호화 적용됨)");

        for (int i = 1; i <= userCount; i++) {
            // 1. 유저 생성 및 비밀번호 암호화
            String rawPassword = "password123";
            User user = User.builder()
                    .loginId("test_id_" + i)           // 자체 로그인 ID
                    .email("testuser" + i + "@example.com")
                    .name("TestName" + i)              // nullable = false 대응
                    .nickname("Tester-" + i)           // nullable = false 대응
                    .password(passwordEncoder.encode(rawPassword))
                    .isPrivate(false)
                    .build();
            user = userRepository.save(user);


            // 2. 앨범 생성
            Album album = new Album();
            album.setTitle("Test Album " + i);
            album.setUser(user);
            album = albumRepository.save(album);

            // 3. 프로젝트 생성
            Project project = Project.builder()
                    .name("Load Test Project " + i)
                    .user(user)
                    .album(album)
                    .bpm(120)
                    .timeSignature("4/4")
                    .tracks(new ArrayList<>())
                    .build();
            project = projectRepository.save(project);

            // 이후 Track 및 Clip 생성 로직은 동일합니다...
            for (int t = 1; t <= tracksPerProject; t++) {
                Track track = new Track();
                track.setName("Track " + t);
                track.setProject(project);
                track.setType(TrackType.AUDIO);
                track.setColor("#" + Integer.toHexString((int) (Math.random() * 0xFFFFFF)));
                track.setVolume(0.0);
                track.setOrderIndex(t);
                track = trackRepository.save(track);

                for (int c = 1; c <= clipsPerTrack; c++) {
                    int fileNum = ((i * t * c) % totalFiles) + 1;
                    String fileName = String.format("test_audio_%04d.wav", fileNum);

                    Date expiration = new Date();
                    expiration.setTime(expiration.getTime() + 1000 * 60 * 60 * 24);
                    String url = s3Client.generatePresignedUrl(bucketName, fileName, expiration).toString();

                    Clip clip = Clip.builder()
                            .track(track)
                            .name("Clip-" + fileName)
                            .startTime((double) (c * 20))
                            .duration(300.0)
                            .offset(0.0)
                            .sourceUrl(url)
                            .volume(1.0)
                            .pan(0.0)
                            .isMuted(false)
                            .build();
                    clipRepository.save(clip);
                }
            }
            if (i % 10 == 0) log.info("✅ 데이터 생성 진행 중: 유저 {}/{} 완료", i, userCount);
        }
        log.info("🎉 암호화된 비밀번호를 포함한 모든 테스트 데이터 생성이 완료되었습니다.");
    }
}