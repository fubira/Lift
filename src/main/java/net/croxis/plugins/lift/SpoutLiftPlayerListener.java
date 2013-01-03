package net.croxis.plugins.lift;

import org.spout.api.entity.Entity;
import org.spout.api.event.EventHandler;
import org.spout.api.event.Listener;
import org.spout.api.event.cause.PlayerCause;
import org.spout.api.event.player.PlayerInteractEvent;
import org.spout.api.event.player.PlayerInteractEvent.Action;
import org.spout.api.event.player.PlayerLeaveEvent;
import org.spout.api.geo.cuboid.Block;
import org.spout.api.material.block.BlockFace;
import org.spout.vanilla.component.substance.material.Sign;
import org.spout.vanilla.event.cause.DamageCause.DamageType;
import org.spout.vanilla.event.cause.HealthChangeCause;
import org.spout.vanilla.event.entity.EntityDamageEvent;
import org.spout.vanilla.material.VanillaMaterials;

public class SpoutLiftPlayerListener implements Listener{
	private SpoutLift plugin;

	public SpoutLiftPlayerListener(SpoutLift plugin){
		plugin.getEngine().getEventManager().registerEvents(this, plugin);
		this.plugin = plugin;
	}
	
	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent event){
		
		if (event.getAction() == Action.RIGHT_CLICK && event.getPlayer().hasPermission("lift.change")) {
			//TODO: There has got to be an easier way to do this
			Block signBlock = event.getPlayer().getWorld().getBlock(event.getInteractedPoint());
			Block buttonBlock = signBlock.translate(BlockFace.BOTTOM);

			if (signBlock.getMaterial() == VanillaMaterials.WALL_SIGN
                && buttonBlock != null
			    && (buttonBlock.getMaterial() == VanillaMaterials.STONE_BUTTON || buttonBlock.getMaterial() == VanillaMaterials.WOOD_BUTTON)) {

				Sign sign = (Sign) signBlock.getComponent();
				SpoutElevator elevator = SpoutElevatorManager.createLift(buttonBlock);
				
				if (elevator == null){
					plugin.logInfo("Elevator generation returned a null object. Please report circumstances that generated this error.");
					return;
				}
				
				if (elevator.getTotalFloors() < 1){
					// This is just a button and sign, not an elevator.
					return;
				} else if (elevator.getTotalFloors() == 1){
					event.getPlayer().sendMessage(Lift.stringOneFloor);
					return;
				}
				
				event.setCancelled(true);
				
				int currentDestinationInt = 1;
				Floor currentFloor = elevator.getFloorFromY(buttonBlock.getY());
				String[] newText = new String[4];
				
				newText[0] = Lift.stringCurrentFloor;
				newText[1] = Integer.toString(currentFloor.getFloor());
				newText[2] = "";
				newText[3] = "";
				try{
					String[] splits = sign.getText()[2].split(": ");
					currentDestinationInt = Integer.parseInt(splits[1]);	
				} catch (Exception e){
					currentDestinationInt = 0;
					plugin.logDebug("non Valid previous destination");
				}
				currentDestinationInt++;
				if (currentDestinationInt == currentFloor.getFloor()){
					currentDestinationInt++;
					plugin.logDebug("Skipping current floor");
				}
				// The following line MAY be what causes a potential bug for max floors
				if (currentDestinationInt > elevator.getTotalFloors()){
					currentDestinationInt = 1;
					if (currentFloor.getFloor() == 1)
						currentDestinationInt = 2;
					plugin.logDebug("Rotating back to first floor");
				}
				newText[2] = Lift.stringDestination + " " + Integer.toString(currentDestinationInt);
				newText[3] = elevator.getFloorFromN(currentDestinationInt).getName();
				
				sign.setText(newText, new PlayerCause(event.getPlayer()));
				plugin.logDebug("Completed sign update");
			}
		}
	}
	
	@EventHandler
	public void onEntityDamage(EntityDamageEvent e){
		if(e.getCause() == HealthChangeCause.DAMAGE && e.getDamageType() == DamageType.FALL){
			Entity faller = e.getEntity();
			if(SpoutElevatorManager.fallers.contains(faller)){
				e.setCancelled(true);
				//ElevatorManager.fallers.remove(faller);
			}
		}
	}
	
	@EventHandler
	public void onPlayerQuit(PlayerLeaveEvent event){
		SpoutElevatorManager.removePlayer(event.getPlayer());
	}
}
