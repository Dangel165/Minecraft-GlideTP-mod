package com.tpmod.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.tpmod.data.TPDataManager;
import com.tpmod.data.TPLocation;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.Map;

public class TPCommands {
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // /tp <x> <y> <z> - 좌표로 텔레포트
        dispatcher.register(Commands.literal("tp")
            .requires(source -> source.hasPermission(2))
            .then(Commands.argument("pos", Vec3Argument.vec3())
                .executes(ctx -> teleportToCoords(ctx))));
        
        // /tpto <player> - 플레이어에게 텔레포트
        dispatcher.register(Commands.literal("tpto")
            .requires(source -> source.hasPermission(2))
            .then(Commands.argument("player", EntityArgument.player())
                .executes(ctx -> teleportToPlayer(ctx))));
        
        // /sethome [name] - 홈 설정
        dispatcher.register(Commands.literal("sethome")
            .executes(ctx -> setHome(ctx, "home"))
            .then(Commands.argument("name", StringArgumentType.string())
                .executes(ctx -> setHome(ctx, StringArgumentType.getString(ctx, "name")))));
        
        // /home [name] - 홈으로 이동
        dispatcher.register(Commands.literal("home")
            .executes(ctx -> teleportHome(ctx, "home"))
            .then(Commands.argument("name", StringArgumentType.string())
                .executes(ctx -> teleportHome(ctx, StringArgumentType.getString(ctx, "name")))));
        
        // /delhome <name> - 홈 삭제
        dispatcher.register(Commands.literal("delhome")
            .then(Commands.argument("name", StringArgumentType.string())
                .executes(ctx -> deleteHome(ctx))));
        
        // /homes - 홈 목록
        dispatcher.register(Commands.literal("homes")
            .executes(ctx -> listHomes(ctx)));
        
        // /setwarp <name> - 워프 설정 (관리자)
        dispatcher.register(Commands.literal("setwarp")
            .requires(source -> source.hasPermission(2))
            .then(Commands.argument("name", StringArgumentType.string())
                .executes(ctx -> setWarp(ctx))));
        
        // /warp <name> - 워프로 이동
        dispatcher.register(Commands.literal("warp")
            .then(Commands.argument("name", StringArgumentType.string())
                .executes(ctx -> teleportWarp(ctx))));
        
        // /delwarp <name> - 워프 삭제 (관리자)
        dispatcher.register(Commands.literal("delwarp")
            .requires(source -> source.hasPermission(2))
            .then(Commands.argument("name", StringArgumentType.string())
                .executes(ctx -> deleteWarp(ctx))));
        
        // /warps - 워프 목록
        dispatcher.register(Commands.literal("warps")
            .executes(ctx -> listWarps(ctx)));
        
        // /tphelp - 도움말
        dispatcher.register(Commands.literal("tphelp")
            .executes(ctx -> showHelp(ctx)));
    }
    
    private static int teleportToCoords(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer player = ctx.getSource().getPlayerOrException();
            Vec3 pos = Vec3Argument.getVec3(ctx, "pos");
            
            player.teleportTo(pos.x, pos.y, pos.z);
            ctx.getSource().sendSuccess(() -> 
                Component.literal(String.format("§a%.1f, %.1f, %.1f로 이동했습니다", pos.x, pos.y, pos.z)), false);
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("§c텔레포트 실패"));
            return 0;
        }
    }
    
    private static int teleportToPlayer(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer player = ctx.getSource().getPlayerOrException();
            ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
            
            player.teleportTo(target.serverLevel(), target.getX(), target.getY(), target.getZ(), 
                target.getYRot(), target.getXRot());
            ctx.getSource().sendSuccess(() -> 
                Component.literal("§a" + target.getName().getString() + "에게 이동했습니다"), false);
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("§c플레이어를 찾을 수 없습니다"));
            return 0;
        }
    }
    
    private static int setHome(CommandContext<CommandSourceStack> ctx, String name) {
        try {
            ServerPlayer player = ctx.getSource().getPlayerOrException();
            TPLocation location = new TPLocation(
                player.getX(), player.getY(), player.getZ(),
                player.getYRot(), player.getXRot(),
                player.level().dimension().location().toString()
            );
            
            TPDataManager.setHome(player, name, location);
            ctx.getSource().sendSuccess(() -> 
                Component.literal("§a홈 '" + name + "'을(를) 설정했습니다"), false);
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("§c홈 설정 실패"));
            return 0;
        }
    }
    
    private static int teleportHome(CommandContext<CommandSourceStack> ctx, String name) {
        try {
            ServerPlayer player = ctx.getSource().getPlayerOrException();
            TPLocation location = TPDataManager.getHome(player, name);
            
            if (location == null) {
                ctx.getSource().sendFailure(Component.literal("§c홈 '" + name + "'을(를) 찾을 수 없습니다"));
                return 0;
            }
            
            player.teleportTo(location.getX(), location.getY(), location.getZ());
            player.setYRot(location.getYaw());
            player.setXRot(location.getPitch());
            
            ctx.getSource().sendSuccess(() -> 
                Component.literal("§a홈 '" + name + "'(으)로 이동했습니다"), false);
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("§c텔레포트 실패"));
            return 0;
        }
    }
    
    private static int deleteHome(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer player = ctx.getSource().getPlayerOrException();
            String name = StringArgumentType.getString(ctx, "name");
            
            TPDataManager.deleteHome(player, name);
            ctx.getSource().sendSuccess(() -> 
                Component.literal("§c홈 '" + name + "'을(를) 삭제했습니다"), false);
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("§c홈 삭제 실패"));
            return 0;
        }
    }
    
    private static int listHomes(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer player = ctx.getSource().getPlayerOrException();
            Map<String, TPLocation> homes = TPDataManager.getPlayerHomes(player);
            
            if (homes.isEmpty()) {
                ctx.getSource().sendSuccess(() -> 
                    Component.literal("§e설정된 홈이 없습니다"), false);
            } else {
                ctx.getSource().sendSuccess(() -> 
                    Component.literal("§6=== 내 홈 목록 ==="), false);
                homes.forEach((name, loc) -> {
                    ctx.getSource().sendSuccess(() -> 
                        Component.literal("§f" + name + ": " + loc.toString()), false);
                });
            }
            return 1;
        } catch (Exception e) {
            return 0;
        }
    }
    
    private static int setWarp(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer player = ctx.getSource().getPlayerOrException();
            String name = StringArgumentType.getString(ctx, "name");
            
            TPLocation location = new TPLocation(
                player.getX(), player.getY(), player.getZ(),
                player.getYRot(), player.getXRot(),
                player.level().dimension().location().toString()
            );
            
            TPDataManager.setWarp(name, location);
            ctx.getSource().sendSuccess(() -> 
                Component.literal("§a워프 '" + name + "'을(를) 설정했습니다"), false);
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("§c워프 설정 실패"));
            return 0;
        }
    }
    
    private static int teleportWarp(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer player = ctx.getSource().getPlayerOrException();
            String name = StringArgumentType.getString(ctx, "name");
            TPLocation location = TPDataManager.getWarp(name);
            
            if (location == null) {
                ctx.getSource().sendFailure(Component.literal("§c워프 '" + name + "'을(를) 찾을 수 없습니다"));
                return 0;
            }
            
            player.teleportTo(location.getX(), location.getY(), location.getZ());
            player.setYRot(location.getYaw());
            player.setXRot(location.getPitch());
            
            ctx.getSource().sendSuccess(() -> 
                Component.literal("§a워프 '" + name + "'(으)로 이동했습니다"), false);
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("§c텔레포트 실패"));
            return 0;
        }
    }
    
    private static int deleteWarp(CommandContext<CommandSourceStack> ctx) {
        String name = StringArgumentType.getString(ctx, "name");
        TPDataManager.deleteWarp(name);
        ctx.getSource().sendSuccess(() -> 
            Component.literal("§c워프 '" + name + "'을(를) 삭제했습니다"), false);
        return 1;
    }
    
    private static int listWarps(CommandContext<CommandSourceStack> ctx) {
        Map<String, TPLocation> warps = TPDataManager.getAllWarps();
        
        if (warps.isEmpty()) {
            ctx.getSource().sendSuccess(() -> 
                Component.literal("§e설정된 워프가 없습니다"), false);
        } else {
            ctx.getSource().sendSuccess(() -> 
                Component.literal("§6=== 워프 목록 ==="), false);
            warps.forEach((name, loc) -> {
                ctx.getSource().sendSuccess(() -> 
                    Component.literal("§f" + name + ": " + loc.toString()), false);
            });
        }
        return 1;
    }
    
    private static int showHelp(CommandContext<CommandSourceStack> ctx) {
        ctx.getSource().sendSuccess(() -> Component.literal("§6=== TP Mod 명령어 ==="), true);
        ctx.getSource().sendSuccess(() -> Component.literal("§f/tp <x> <y> <z> §7- 좌표로 텔레포트"), true);
        ctx.getSource().sendSuccess(() -> Component.literal("§f/tpto <player> §7- 플레이어에게 텔레포트"), true);
        ctx.getSource().sendSuccess(() -> Component.literal("§f/sethome [name] §7- 홈 설정 (기본: home)"), true);
        ctx.getSource().sendSuccess(() -> Component.literal("§f/home [name] §7- 홈으로 이동"), true);
        ctx.getSource().sendSuccess(() -> Component.literal("§f/delhome <name> §7- 홈 삭제"), true);
        ctx.getSource().sendSuccess(() -> Component.literal("§f/homes §7- 내 홈 목록 보기"), true);
        ctx.getSource().sendSuccess(() -> Component.literal("§f/setwarp <name> §7- 워프 설정 (관리자)"), true);
        ctx.getSource().sendSuccess(() -> Component.literal("§f/warp <name> §7- 워프로 이동"), true);
        ctx.getSource().sendSuccess(() -> Component.literal("§f/delwarp <name> §7- 워프 삭제 (관리자)"), true);
        ctx.getSource().sendSuccess(() -> Component.literal("§f/warps §7- 워프 목록 보기"), true);
        ctx.getSource().sendSuccess(() -> Component.literal("§f/tphelp §7- 이 도움말 보기"), true);
        return 1;
    }
}
