package com.example.monsterhunter.repository;

import com.example.monsterhunter.entity.Weapon;
import org.springframework.data.jpa.repository.JpaRepository;

/** 給 StoreService 刪除「強化時被換下來的舊武器」用；角色被刪時的武器由資料庫 trigger 處理（見 V4 migration）。 */
public interface WeaponRepository extends JpaRepository<Weapon, Long> {
}
