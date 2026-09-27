package com.example.monsterhunter.controller;

import com.example.monsterhunter.dto.PlayerResponse;
import com.example.monsterhunter.dto.PotionType;
import com.example.monsterhunter.security.UserPrincipal;
import com.example.monsterhunter.service.StoreService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 商店的 API 入口：買藥水、強化武器。需要登入，花的是自己獵人的錢、
 * 強化的也是自己獵人身上裝備的武器，實際規則（價格、強化幅度）寫在 StoreService。
 */
@RestController
@RequestMapping("/api/store")
public class StoreController {

    private final StoreService storeService;

    public StoreController(StoreService storeService) {
        this.storeService = storeService;
    }

    @PostMapping("/potion")
    public PlayerResponse buyPotion(@AuthenticationPrincipal UserPrincipal user, @RequestParam PotionType type) {
        return new PlayerResponse(storeService.buyPotion(user.getId(), type));
    }

    @PostMapping("/upgrade-weapon")
    public PlayerResponse upgradeWeapon(@AuthenticationPrincipal UserPrincipal user) {
        return new PlayerResponse(storeService.upgradeWeapon(user.getId()));
    }
}
