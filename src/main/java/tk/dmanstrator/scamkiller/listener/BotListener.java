package tk.dmanstrator.scamkiller.listener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.PermissionOverride;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.exceptions.PermissionException;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.utils.cache.MemberCacheView;
import net.dv8tion.jda.api.utils.cache.SnowflakeCacheView;

public class BotListener extends ListenerAdapter  {
	
	private final static Logger LOGGER = Logger.getLogger(BotListener.class.getName());
	
	private static final String DEFAULT_BAN_REASON = "[Auto-Detection] Unwanted Scammer";
	private static final String CROSSBAN_REASON = DEFAULT_BAN_REASON + " (Crossban)";
	private static final String ONBOOT_REASON = DEFAULT_BAN_REASON + " (Ban on Bot Start)";
	
	private static final List<Permission> HIGH_PERMISSIONS = Arrays.asList(Permission.ADMINISTRATOR, Permission.BAN_MEMBERS,
			Permission.MANAGE_SERVER, Permission.KICK_MEMBERS);  // list because the order matters

	private final List<Long> unwantedIds = initBanIds();
	private final List<String> unwantedNames = initBanNames();
	
	private void checkName(Guild guild, User user)  {
		String username = user.getName();
		if (unwantedNames.contains(username.toLowerCase()))  {
			ban(guild, user);
			crossban(guild.getJDA().getGuildCache(), user);
		}
	}

	private void crossban(SnowflakeCacheView<Guild> guildCache, User user) {
		guildCache.forEachUnordered(guild -> {
			Member member = guild.getMember(user);
			if (member != null)  {
				ban(member, CROSSBAN_REASON);
			}
		});
		unwantedIds.add(user.getIdLong());
		
	}

	private void ban(Guild guild, User userToBan)  {
		ban(guild.getMember(userToBan));
	}
	
	private void ban(Member memberToBan)  {
		ban(memberToBan, DEFAULT_BAN_REASON);
	}
	
	private void ban(Member memberToBan, String reason)  {
		memberToBan.ban(0, reason).queue(success ->  {
			LOGGER.log(Level.INFO,
					String.format("%#s (Id: %d) was banned from '%s'. Reason: %s",
							memberToBan, memberToBan.getIdLong(), memberToBan.getGuild().getName(), reason));
		}, failure ->  {
			handleError(failure, memberToBan.getGuild(), "banning");
		});

	}
	
	private void handleError(Throwable failure, Guild guild, String action)  {
		String info;
		if (failure instanceof PermissionException)  {
			PermissionException pe = (PermissionException) failure;
            Permission missingPermission = pe.getPermission();
            if (missingPermission == Permission.UNKNOWN)  {
				info = "PermissionError: " + pe.getMessage();
            }  else  {
				info = "I am missing the following permission for " + action + ": " + missingPermission;
            }
		}  else  {
			info = "Unexpected Error: " + failure.getMessage();
		}

		Member owner = guild.getOwner();
		if (owner != null)  {
			owner.getUser().openPrivateChannel().queue(pc -> {
				pc.sendMessage(info).queue(null, dmFail ->  {
					postErrorInPublicChannel(guild,
							"Problem occured but DMs are blocked by server owner." + System.lineSeparator() + info);
				});
			});
		}  else  {
			postErrorInPublicChannel(guild, "Owner is unreachable." + System.lineSeparator() + info);
		}
	}

	private void postErrorInPublicChannel(Guild guild, String errorMsg) {
		TextChannel channel = getMostPrivateChannel(guild);
		if (channel == null)  {
			channel = guild.getDefaultChannel();
			if (channel == null)  {
				Member owner = guild.getOwner();
				String ownerName = owner != null ? String.format("%#s", owner) : "<deleted owner>";
				LOGGER.log(Level.WARNING,
						String.format("An error has occured and I can't inform the Guild '%s' (Id: %d)."
								+ "Both DMs by owner '%s' are blocked / no owner exists as well no writtable channel exists.",
								guild.getName(), guild.getIdLong(), ownerName));
				return;
			}
		}
		channel.sendMessage(errorMsg).queue();
	}

	private TextChannel getMostPrivateChannel(Guild guild) {
		Role everyoneRole = guild.getPublicRole();
		List<TextChannel> talkablePrivateChannels = guild.getTextChannelCache().stream()
			.filter(TextChannel::canTalk)
			.filter(tc -> {
				PermissionOverride publicOverride = tc.getPermissionOverride(everyoneRole);
				// no override for public role? -> open channel
				if (publicOverride == null)  {
					return false;
				}

				boolean readDenied = publicOverride.getDenied().contains(Permission.MESSAGE_READ);
				// has overrides but has read permissions
				if (!readDenied)  {
					return false;
				}

				return true;
			})
			.collect(Collectors.toList());

		Member owner = guild.getOwner();

		List<TextChannel> privateChannels = talkablePrivateChannels.stream()
			.filter(tc -> {
				if (owner != null)  {
					PermissionOverride ownerOverride = tc.getPermissionOverride(owner);
					// owner has an override -> looks promising
					if (ownerOverride != null)  {
						return true;
					}
				}

				// everyone disabled and no admin override -> let's check for other perms
				List<PermissionOverride> permissionOverrides = tc.getPermissionOverrides();

				for (Permission perm : HIGH_PERMISSIONS)  {
					// Any member with override has high permission -> looks promising
					boolean memberAdminOverride = permissionOverrides.stream()
						.filter(PermissionOverride::isMemberOverride)
						.anyMatch(po -> po.getMember().hasPermission(perm));
					if (memberAdminOverride)  {
						return true;
					}

					// Any role with override has high permission -> looks promising
					boolean roleAdminOverride = permissionOverrides.stream()
						.filter(PermissionOverride::isRoleOverride)
						.anyMatch(po -> po.getRole().hasPermission(perm));
					if (roleAdminOverride)  {
						return true;
					}
				}
				return false;
			})
			// the less overrides it has the better it is
			.sorted(Comparator.comparingInt(tc -> tc.getPermissionOverrides().size()))
			.collect(Collectors.toList());
		return privateChannels.isEmpty() ? null : talkablePrivateChannels.get(0);
	}

	@Override
	public void onReady(ReadyEvent event) {
		SnowflakeCacheView<Guild> guildCache = event.getJDA().getGuildCache();
		guildCache.forEachUnordered(guild -> {
			// first check for ids
			unwantedIds.stream().map(guild::getMemberById).filter(mem -> mem != null).forEach(mem ->  {
				ban(mem, ONBOOT_REASON);
			});
			// TODO maybe wait for done -> else cache is outdated -> error on banning
			
			// check for names
			MemberCacheView memberCache = guild.getMemberCache();
			memberCache.forEachUnordered(member -> {
				checkName(guild, member.getUser());
			});
		});
	}
	
	@Override
	public void onGuildMemberJoin(GuildMemberJoinEvent event) {
		checkName(event.getGuild(), event.getUser());
	}
	
	private List<String> initBanNames() {
		// TODO read from file
		return new ArrayList<>();
	}
	
	private List<Long> initBanIds() {
		// TODO read from file
		return new ArrayList<>();
	}

}
