package com.joyhill.demo.config;

import com.joyhill.demo.common.util.KoreanNameGenerator;
import com.joyhill.demo.common.util.PhoneUtils;
import com.joyhill.demo.domain.*;
import com.joyhill.demo.repository.FamMemberRepository;
import com.joyhill.demo.repository.FamRepository;
import com.joyhill.demo.repository.TeamRoleRepository;
import com.joyhill.demo.repository.UserRepository;
import com.joyhill.demo.repository.VillageRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initData(UserRepository userRepository, VillageRepository villageRepository, FamRepository famRepository,
                               FamMemberRepository famMemberRepository, TeamRoleRepository teamRoleRepository,
                               PasswordEncoder passwordEncoder) {
        return args -> {
            if (userRepository.count() > 0) {
                return;
            }

            Village village = villageRepository.save(new Village(KoreanNameGenerator.villageName("홍성인"), "홍성인"));
            Fam leaderFam = famRepository.save(new Fam(KoreanNameGenerator.famName("김민준"), village.getName(), "김민준"));

            createUser(userRepository, passwordEncoder, "김민준", "010-1111-2222", "950315", Role.leader, leaderFam.getName(), village.getName());
            createUser(userRepository, passwordEncoder, "박청년", "010-9999-0000", "001225", Role.member, leaderFam.getName(), village.getName());
            createUser(userRepository, passwordEncoder, "홍성인", "010-3333-4444", "881020", Role.village_leader, leaderFam.getName(), village.getName());
            createUser(userRepository, passwordEncoder, "정교역자", "010-5555-6666", "750601", Role.pastor, null, null);
            User admin = createUser(userRepository, passwordEncoder, "관리자", "010-7777-8888", "700101", Role.admin, null, null);

            FamMember leaderMember = new FamMember();
            leaderMember.setName("김민준");
            leaderMember.setFamName(leaderFam.getName());
            leaderMember.setPhone(PhoneUtils.normalize("010-1111-2222"));
            leaderMember.setRole(Role.leader);
            famMemberRepository.save(leaderMember);

            FamMember member = new FamMember();
            member.setName("박청년");
            member.setFamName(leaderFam.getName());
            member.setPhone(PhoneUtils.normalize("010-9999-0000"));
            member.setRole(Role.member);
            famMemberRepository.save(member);

            teamRoleRepository.save(new TeamRole(admin.getId(), "새가족팀", true));
        };
    }

    private User createUser(UserRepository userRepository, PasswordEncoder passwordEncoder, String name, String phone, String birth,
                            Role role, String famName, String villageName) {
        User user = new User();
        user.setName(name);
        user.setPhone(PhoneUtils.normalize(phone));
        user.setBirth(birth);
        user.setPassword(passwordEncoder.encode(birth));
        user.setRole(role);
        user.setFamName(famName);
        user.setVillageName(villageName);
        return userRepository.save(user);
    }
}
