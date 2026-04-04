package com.joyhill.demo.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false, unique = true, length = 20)
    private String phone;

    // 비밀번호 변경 전에는 null, 변경 후 BCrypt 해시 저장
    @Column(length = 255)
    private String password;

    // 생년월일 6자리 평문 (최초 로그인 인증에 사용)
    @Column(nullable = false, length = 6)
    private String birth;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role = Role.member;

    @Column(name = "fam_name", length = 50)
    private String famName;

    @Column(name = "village_name", length = 50)
    private String villageName;

    @Column(name = "refresh_token", columnDefinition = "TEXT")
    private String refreshToken;

    @Column(name = "is_password_changed", nullable = false)
    private boolean passwordChanged = false;

    public Long getId() { return id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getBirth() { return birth; }
    public void setBirth(String birth) { this.birth = birth; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public String getFamName() { return famName; }
    public void setFamName(String famName) { this.famName = famName; }

    public String getVillageName() { return villageName; }
    public void setVillageName(String villageName) { this.villageName = villageName; }

    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }

    public boolean isPasswordChanged() { return passwordChanged; }
    public void setPasswordChanged(boolean passwordChanged) { this.passwordChanged = passwordChanged; }
}
