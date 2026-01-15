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
import java.util.List;

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
    private final PasswordEncoder passwordEncoder;

    @Value("${cloud.aws.s3.bucket}")
    private String bucketName;

    /**
     * 1. 테스트 유저만 대량 생성
     */
    @Transactional
    public void createBulkUsers(int count) {
        log.info("👤 테스트 유저 {}명 생성 시작", count);
        String rawPassword = "password123";
        String encodedPassword = passwordEncoder.encode(rawPassword);

        for (int i = 1; i <= count; i++) {
            User user = User.builder()
                    .loginId("test_id_" + i)
                    .email("testuser" + i + "@example.com")
                    .name("TestName" + i)
                    .nickname("Tester-" + i)
                    .password(encodedPassword)
                    .isPrivate(false)
                    .build();
            userRepository.save(user);
        }
        log.info("✅ 유저 생성 완료");
    }

    /**
     * 2. 생성된 테스트 유저들에게 프로젝트/트랙/클립 데이터 할당
     */
    @Transactional
    public void createProjectDataForTestUsers() {
        int totalFiles = 400;
        int tracksPerProject = 15;
        int clipsPerTrack = 5;

        // "test_id_"로 시작하는 유저들만 가져옴
        List<User> testUsers = userRepository.findAll().stream()
                .filter(u -> u.getLoginId() != null && u.getLoginId().startsWith("test_id_"))
                .toList();

        log.info("🎹 {}명의 유저에게 프로젝트 데이터 생성 시작", testUsers.size());

        int userIdx = 1;
        for (User user : testUsers) {
            // 앨범 생성
            Album album = new Album();
            album.setTitle("Test Album " + userIdx);
            album.setUser(user);
            albumRepository.save(album);

            // 프로젝트 생성
            Project project = Project.builder()
                    .name("Load Test Project " + userIdx)
                    .user(user)
                    .album(album)
                    .bpm(120)
                    .timeSignature("4/4")
                    .tracks(new ArrayList<>())
                    .build();
            projectRepository.save(project);

            // 트랙 및 클립 생성
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
                    int fileNum = ((userIdx * t * c) % totalFiles) + 1;
                    String fileName = String.format("test_audio_%04d.wav", fileNum);

                    Date expiration = new Date();
                    expiration.setTime(expiration.getTime() + 1000 * 60 * 60 * 24);
                    String url = s3Client.generatePresignedUrl(bucketName, fileName, expiration).toString();

                    Clip clip = Clip.builder()
                            .track(track)
                            .name("Clip-" + fileName)
                            .startTime((double) (c * 20))
                            .duration(300.0)
                            .sourceUrl(url)
                            .volume(1.0)
                            .build();
                    clipRepository.save(clip);
                }
            }
            if (userIdx % 10 == 0) log.info("✅ 프로젝트 데이터 생성 중... ({}/{})", userIdx, testUsers.size());
            userIdx++;
        }
        log.info("🎉 모든 프로젝트 데이터 생성 완료");
    }
}