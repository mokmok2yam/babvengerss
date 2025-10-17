package com.example.babvengerss.controller;

import com.example.babvengerss.domain.User;
import com.example.babvengerss.dto.LoginResponse; // 추가
import com.example.babvengerss.repository.UserRepository;
import com.example.babvengerss.util.JwtUtil; // 추가
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserController {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    @PostMapping("/signup")
    public ResponseEntity<String> signup(@RequestBody User user) {
        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            return ResponseEntity.badRequest().body("이미 존재하는 사용자입니다.");
        }
        userRepository.save(user);
        return ResponseEntity.ok("회원가입 성공");
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody User loginRequest) {
        return userRepository.findByUsername(loginRequest.getUsername())
                .map(foundUser -> {
                    // 2. 사용자가 존재하면, 비밀번호가 일치하는지 확인
                    if (foundUser.getPassword().equals(loginRequest.getPassword())) {
                        // 3. 비밀번호가 일치하면 토큰을 생성
                        final String token = jwtUtil.generateToken(foundUser.getUsername());
                        LoginResponse loginResponse = new LoginResponse(
                                foundUser.getId(),
                                foundUser.getUsername(),
                                foundUser.getNickname(),
                                token
                        );
                        return ResponseEntity.ok(loginResponse);
                    } else {
                        // 비밀번호가 틀렸을 경우
                        return ResponseEntity.status(401).body("아이디 또는 비밀번호 오류");
                    }
                })
                // 1. 사용자가 존재하지 않을 경우
                .orElse(ResponseEntity.status(401).body("아이디 또는 비밀번호 오류"));
    }
}