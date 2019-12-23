package com.yyon.grapplinghook.items;

import java.util.HashMap;
import java.util.List;

import org.lwjgl.input.Keyboard;

import com.yyon.grapplinghook.ClientProxyClass;
import com.yyon.grapplinghook.CommonProxyClass;
import com.yyon.grapplinghook.GrappleCustomization;
import com.yyon.grapplinghook.grapplemod;
import com.yyon.grapplinghook.vec;
import com.yyon.grapplinghook.entities.grappleArrow;
import com.yyon.grapplinghook.network.DetachSingleHookMessage;
import com.yyon.grapplinghook.network.GrappleDetachMessage;
import com.yyon.grapplinghook.network.KeypressMessage;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.EnumAction;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;


/*
 * This file is part of GrappleMod.

    GrappleMod is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    GrappleMod is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with GrappleMod.  If not, see <http://www.gnu.org/licenses/>.
 */

public class grappleBow extends Item implements KeypressItem {
	public static HashMap<Entity, grappleArrow> grapplearrows1 = new HashMap<Entity, grappleArrow>();
	public static HashMap<Entity, grappleArrow> grapplearrows2 = new HashMap<Entity, grappleArrow>();
	
	public grappleBow() {
		super();
		maxStackSize = 1;
		setFull3D();
		setUnlocalizedName("grapplinghook");
		
		this.setMaxDamage(500);
		
		setCreativeTab(CreativeTabs.TRANSPORTATION);
		
		MinecraftForge.EVENT_BUS.register(this);
	}

    @Override
	public int getMaxItemUseDuration(ItemStack par1ItemStack)
	{
		return 72000;
	}
	
	public boolean hasArrow(Entity entity) {
		grappleArrow arrow1 = getArrowLeft(entity);
		grappleArrow arrow2 = getArrowRight(entity);
		return (arrow1 != null) || (arrow2 != null);
	}
	
	@Override
	public boolean getIsRepairable(ItemStack toRepair, ItemStack repair) {
        ItemStack mat = new ItemStack(Items.LEATHER, 1);
        if (mat != null && net.minecraftforge.oredict.OreDictionary.itemMatches(mat, repair, false)) return true;
        return super.getIsRepairable(toRepair, repair);
	}

	public void setArrowLeft(Entity entity, grappleArrow arrow) {
		grappleBow.grapplearrows1.put(entity, arrow);
	}
	public void setArrowRight(Entity entity, grappleArrow arrow) {
		grappleBow.grapplearrows2.put(entity, arrow);
	}
	public grappleArrow getArrowLeft(Entity entity) {
		if (grappleBow.grapplearrows1.containsKey(entity)) {
			grappleArrow arrow = grappleBow.grapplearrows1.get(entity);
			if (arrow != null && !arrow.isDead) {
				return arrow;
			}
		}
		return null;
	}
	public grappleArrow getArrowRight(Entity entity) {
		if (grappleBow.grapplearrows2.containsKey(entity)) {
			grappleArrow arrow = grappleBow.grapplearrows2.get(entity);
			if (arrow != null && !arrow.isDead) {
				return arrow;
			}
		}
		return null;
	}	
	
	public void dorightclick(ItemStack stack, World worldIn, EntityLivingBase entityLiving, boolean righthand) {
        if (!worldIn.isRemote) {
        	throwBoth(stack, worldIn, entityLiving, righthand);
    	}
	}
	
	public void throwBoth(ItemStack stack, World worldIn, EntityLivingBase entityLiving, boolean righthand) {
		grappleArrow arrow_left = getArrowLeft(entityLiving);
		grappleArrow arrow_right = getArrowRight(entityLiving);

		if (arrow_left != null || arrow_right != null) {
    		if (arrow_left != null) detachLeft(entityLiving);
    		if (arrow_right != null) detachRight(entityLiving);
    		return;
		}
		
		throwLeft(stack, worldIn, entityLiving, righthand);
		throwRight(stack, worldIn, entityLiving, righthand);

		stack.damageItem(1, entityLiving);
        worldIn.playSound((EntityPlayer)null, entityLiving.posX, entityLiving.posY, entityLiving.posZ, SoundEvents.ENTITY_ARROW_SHOOT, SoundCategory.NEUTRAL, 1.0F, 1.0F / (itemRand.nextFloat() * 0.4F + 1.2F) + 2.0F * 0.5F);
	}
	
	public void throwLeft(ItemStack stack, World worldIn, EntityLivingBase entityLiving, boolean righthand) {
    	GrappleCustomization custom = this.getCustomization(stack);
    	
  		double angle = custom.angle;
  		double verticalangle = custom.verticalthrowangle;
  		
  		if (entityLiving.isSneaking()) {
  			angle = custom.sneakingangle;
  			verticalangle = custom.sneakingverticalthrowangle;
  		}
  		
    	if (!custom.doublehook || angle == 0) {
    	} else {
      		EntityLivingBase player = entityLiving;
      		
      		vec anglevec = new vec(0,0,1).rotate_yaw(Math.toRadians(-angle)).rotate_pitch(Math.toRadians(verticalangle));
      		anglevec = anglevec.rotate_pitch(Math.toRadians(-player.rotationPitch));
      		anglevec = anglevec.rotate_yaw(Math.toRadians(player.rotationYaw));
	        float velx = -MathHelper.sin((float) anglevec.getYaw() * 0.017453292F) * MathHelper.cos((float) anglevec.getPitch() * 0.017453292F);
	        float vely = -MathHelper.sin((float) anglevec.getPitch() * 0.017453292F);
	        float velz = MathHelper.cos((float) anglevec.getYaw() * 0.017453292F) * MathHelper.cos((float) anglevec.getPitch() * 0.017453292F);
			grappleArrow entityarrow = this.createarrow(stack, worldIn, entityLiving, false, true);// new grappleArrow(worldIn, player, false);
//            entityarrow.shoot(player, (float) anglevec.getPitch(), (float)anglevec.getYaw(), 0.0F, entityarrow.getVelocity(), 0.0F);
	        float extravelocity = (float) vec.motionvec(entityLiving).dist_along(new vec(velx, vely, velz));
	        if (extravelocity < 0) { extravelocity = 0; }
	        entityarrow.shoot((double) velx, (double) vely, (double) velz, entityarrow.getVelocity() + extravelocity, 0.0F);
            
			worldIn.spawnEntity(entityarrow);
			setArrowLeft(entityLiving, entityarrow);    			
    	}
	}
	
	public void throwRight(ItemStack stack, World worldIn, EntityLivingBase entityLiving, boolean righthand) {
    	GrappleCustomization custom = this.getCustomization(stack);
    	
  		double angle = custom.angle;
  		double verticalangle = custom.verticalthrowangle;
  		if (entityLiving.isSneaking()) {
  			angle = custom.sneakingangle;
  			verticalangle = custom.sneakingverticalthrowangle;
  		}
  		
    	if (!custom.doublehook || angle == 0) {
			grappleArrow entityarrow = this.createarrow(stack, worldIn, entityLiving, righthand, false);
      		vec anglevec = new vec(0,0,1).rotate_pitch(Math.toRadians(verticalangle));
      		anglevec = anglevec.rotate_pitch(Math.toRadians(-entityLiving.rotationPitch));
      		anglevec = anglevec.rotate_yaw(Math.toRadians(entityLiving.rotationYaw));
	        float velx = -MathHelper.sin((float) anglevec.getYaw() * 0.017453292F) * MathHelper.cos((float) anglevec.getPitch() * 0.017453292F);
	        float vely = -MathHelper.sin((float) anglevec.getPitch() * 0.017453292F);
	        float velz = MathHelper.cos((float) anglevec.getYaw() * 0.017453292F) * MathHelper.cos((float) anglevec.getPitch() * 0.017453292F);
	        float extravelocity = (float) vec.motionvec(entityLiving).dist_along(new vec(velx, vely, velz));
	        if (extravelocity < 0) { extravelocity = 0; }
	        entityarrow.shoot((double) velx, (double) vely, (double) velz, entityarrow.getVelocity() + extravelocity, 0.0F);
			setArrowRight(entityLiving, entityarrow);
			worldIn.spawnEntity(entityarrow);
    	} else {
      		EntityLivingBase player = entityLiving;
      		
      		vec anglevec = new vec(0,0,1).rotate_yaw(Math.toRadians(angle)).rotate_pitch(Math.toRadians(verticalangle));
      		anglevec = anglevec.rotate_pitch(Math.toRadians(-player.rotationPitch));
      		anglevec = anglevec.rotate_yaw(Math.toRadians(player.rotationYaw));
	        float velx = -MathHelper.sin((float) anglevec.getYaw() * 0.017453292F) * MathHelper.cos((float) anglevec.getPitch() * 0.017453292F);
	        float vely = -MathHelper.sin((float) anglevec.getPitch() * 0.017453292F);
	        float velz = MathHelper.cos((float) anglevec.getYaw() * 0.017453292F) * MathHelper.cos((float) anglevec.getPitch() * 0.017453292F);
			grappleArrow entityarrow = this.createarrow(stack, worldIn, entityLiving, true, true);//new grappleArrow(worldIn, player, true);
//            entityarrow.shoot(player, (float) anglevec.getPitch(), (float)anglevec.getYaw(), 0.0F, entityarrow.getVelocity(), 0.0F);
	        float extravelocity = (float) vec.motionvec(entityLiving).dist_along(new vec(velx, vely, velz));
	        if (extravelocity < 0) { extravelocity = 0; }
	        entityarrow.shoot((double) velx, (double) vely, (double) velz, entityarrow.getVelocity() + extravelocity, 0.0F);
            
			worldIn.spawnEntity(entityarrow);
			setArrowRight(entityLiving, entityarrow);
		}
	}
	
	public void detachLeft(EntityLivingBase entityLiving) {
		grappleArrow arrow1 = getArrowLeft(entityLiving);
		
		setArrowLeft(entityLiving, null);
		
		if (arrow1 != null) {
			arrow1.removeServer();
		}
		
		int id = entityLiving.getEntityId();
		
		// remove controller if hook is attached
		if (getArrowRight(entityLiving) == null) {
			grapplemod.sendtocorrectclient(new GrappleDetachMessage(id), id, entityLiving.world);
		} else {
			grapplemod.sendtocorrectclient(new DetachSingleHookMessage(id, arrow1.getEntityId()), id, entityLiving.world);
		}
		
		if (grapplemod.attached.contains(id)) {
			grapplemod.attached.remove(new Integer(id));
		}
	}
	
	public void detachRight(EntityLivingBase entityLiving) {
		grappleArrow arrow2 = getArrowRight(entityLiving);
		
		setArrowRight(entityLiving, null);
		
		if (arrow2 != null) {
			arrow2.removeServer();
		}
		
		int id = entityLiving.getEntityId();
		
		// remove controller if hook is attached
		if (getArrowLeft(entityLiving) == null) {
			grapplemod.sendtocorrectclient(new GrappleDetachMessage(id), id, entityLiving.world);
		} else {
			grapplemod.sendtocorrectclient(new DetachSingleHookMessage(id, arrow2.getEntityId()), id, entityLiving.world);
		}
		
		if (grapplemod.attached.contains(id)) {
			grapplemod.attached.remove(new Integer(id));
		}
	}
	
    public double getAngle(EntityLivingBase entity, ItemStack stack) {
    	GrappleCustomization custom = this.getCustomization(stack);
    	if (entity.isSneaking()) {
    		return custom.sneakingangle;
    	} else {
    		return custom.angle;
    	}
    }
	
	public grappleArrow createarrow(ItemStack stack, World worldIn, EntityLivingBase entityLiving, boolean righthand, boolean isdouble) {
		grappleArrow arrow = new grappleArrow(worldIn, entityLiving, righthand, this.getCustomization(stack), isdouble);
		grapplemod.addarrow(entityLiving.getEntityId(), arrow);
		return arrow;
	}
    
    @Override
    public ActionResult<ItemStack> onItemRightClick(World worldIn, EntityPlayer entityLiving, EnumHand hand)
    {
    	ItemStack stack = entityLiving.getHeldItem(hand);
        if (!worldIn.isRemote) {
	        this.dorightclick(stack, worldIn, entityLiving, hand == EnumHand.MAIN_HAND);
        }
        entityLiving.setActiveHand(hand);
        return new ActionResult<ItemStack>(EnumActionResult.SUCCESS, stack);
    }
	

	@Override
	public void onPlayerStoppedUsing(ItemStack stack, World worldIn,
			EntityLivingBase entityLiving, int timeLeft) {
		if (!worldIn.isRemote) {
//			stack.getSubCompound("grapplemod", true).setBoolean("extended", (this.getArrow(entityLiving, worldIn) != null));
		}
		super.onPlayerStoppedUsing(stack, worldIn, entityLiving, timeLeft);
	}

	/**
	 * returns the action that specifies what animation to play when the items is being used
	 */
    @Override
	public EnumAction getItemUseAction(ItemStack par1ItemStack)
	{
		return EnumAction.NONE;
	}

    @Override
    public boolean onLeftClickEntity(ItemStack stack, EntityPlayer player, Entity entity)
    {
    	return true;
    }
    
	@Override
	public void onCustomKeyDown(ItemStack stack, EntityPlayer player, KeypressItem.Keys key) {
		if (player.world.isRemote) {
			if (key == KeypressItem.Keys.LAUNCHER) {
				if (this.getCustomization(stack).enderstaff) {
					grapplemod.proxy.launchplayer(player);
				}
			} else if (key == KeypressItem.Keys.THROWLEFT || key == KeypressItem.Keys.THROWRIGHT) {
				grapplemod.network.sendToServer(new KeypressMessage(key, true));
			}
		} else {
			if (key == KeypressItem.Keys.THROWLEFT) {
				grappleArrow arrow1 = getArrowLeft(player);

	    		if (arrow1 != null) {
	    			detachLeft(player);
		    		return;
				}
				
				throwLeft(stack, player.world, player, true);

				stack.damageItem(1, player);
		        player.world.playSound((EntityPlayer)null, player.posX, player.posY, player.posZ, SoundEvents.ENTITY_ARROW_SHOOT, SoundCategory.NEUTRAL, 1.0F, 1.0F / (itemRand.nextFloat() * 0.4F + 1.2F) + 2.0F * 0.5F);
			} else if (key == KeypressItem.Keys.THROWRIGHT) {
				grappleArrow arrow2 = getArrowRight(player);

	    		if (arrow2 != null) {
	    			detachRight(player);
		    		return;
				}
				
				throwRight(stack, player.world, player, true);

				stack.damageItem(1, player);
		        player.world.playSound((EntityPlayer)null, player.posX, player.posY, player.posZ, SoundEvents.ENTITY_ARROW_SHOOT, SoundCategory.NEUTRAL, 1.0F, 1.0F / (itemRand.nextFloat() * 0.4F + 1.2F) + 2.0F * 0.5F);
			}

	        System.out.println("Key down");
			System.out.println(key.toString());
		}
	}
	
	@Override
	public void onCustomKeyUp(ItemStack stack, EntityPlayer player, KeypressItem.Keys key) {
		if (player.world.isRemote) {
			if (key == KeypressItem.Keys.THROWLEFT || key == KeypressItem.Keys.THROWRIGHT) {
				grapplemod.network.sendToServer(new KeypressMessage(key, false));
			}
		} else {
			System.out.println("Key up");
			System.out.println(key.toString());
		}
	}
   
    @Override
    public boolean hitEntity(ItemStack stack, EntityLivingBase target, EntityLivingBase attacker)
    {
    	return true;
    }
   
    @Override
    public boolean onBlockStartBreak(ItemStack itemstack, BlockPos k, EntityPlayer player)
    {
      return true;
    }
    
    public GrappleCustomization getCustomization(ItemStack itemstack) {
    	if (itemstack.hasTagCompound()) {
        	GrappleCustomization custom = new GrappleCustomization();
    		custom.loadNBT(itemstack.getTagCompound());
        	return custom;
    	} else {
    		GrappleCustomization custom = this.getDefaultCustomization();

			NBTTagCompound nbt = custom.writeNBT();
			
			itemstack.setTagCompound(nbt);
    		
    		return custom;
    	}
    }
    
    public GrappleCustomization getDefaultCustomization() {
    	return new GrappleCustomization();
    }
    
	@Override
    @SideOnly(Side.CLIENT)
	public void addInformation(ItemStack stack, World world, List<String> list, ITooltipFlag par4)
	{
		GrappleCustomization custom = getCustomization(stack);

		
		if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT)) {
			list.add(grapplemod.proxy.getkeyname(CommonProxyClass.keys.keyBindUseItem) + " - Throw grappling hook");
			list.add(grapplemod.proxy.getkeyname(CommonProxyClass.keys.keyBindUseItem) + " again - Release");
			list.add("Double-" + grapplemod.proxy.getkeyname(CommonProxyClass.keys.keyBindUseItem) + " - Release and throw again");
			list.add(grapplemod.proxy.getkeyname(CommonProxyClass.keys.keyBindForward) + ", " +
					grapplemod.proxy.getkeyname(CommonProxyClass.keys.keyBindLeft) + ", " +
					grapplemod.proxy.getkeyname(CommonProxyClass.keys.keyBindBack) + ", " +
					grapplemod.proxy.getkeyname(CommonProxyClass.keys.keyBindRight) +
					" - Swing");
			list.add(ClientProxyClass.key_jumpanddetach.getDisplayName() + " - Release and jump (while in midair)");
			list.add(ClientProxyClass.key_slow.getDisplayName() + " - Stop swinging");
			list.add((custom.climbkey ? ClientProxyClass.key_climb.getDisplayName() + " + " : "") +
					ClientProxyClass.key_climbup.getDisplayName() + 
					" - Climb up");
			list.add((custom.climbkey ? ClientProxyClass.key_climb.getDisplayName() + " + " : "") +
					ClientProxyClass.key_climbdown.getDisplayName() + 
					" - Climb down");
		} else {
			if (Keyboard.isKeyDown(Keyboard.KEY_LCONTROL)) {
				for (String option : GrappleCustomization.booleanoptions) {
					if (custom.isoptionvalid(option) && custom.getBoolean(option)) {
						list.add(custom.getName(option));
					}
				}
				for (String option : GrappleCustomization.doubleoptions) {
					if (custom.isoptionvalid(option)) {
						list.add(custom.getName(option) + ": " + Math.floor(custom.getDouble(option) * 100) / 100);
					}
				}
			} else {
				if (custom.doublehook) {
					list.add("Double Hook");
				}
				if (custom.motor) {
					if (custom.smartmotor) {
						list.add("Smart Motor");
					} else {
						list.add("Motorized");
					}
				}
				if (custom.enderstaff) {
					list.add("Ender Staff");
				}
				if (custom.attract) {
					list.add("Magnetized");
				}
				if (custom.repel) {
					list.add("Forcefield");
				}
				
				list.add("");
				list.add("(Hold Shift to see controls)");
				list.add("(Hold Control to see full configuration)");
			}
		}
		

	}

	
	@Override
    @SideOnly(Side.CLIENT)
    public ItemStack getDefaultInstance()
    {
        ItemStack stack = new ItemStack(this);
        this.getCustomization(stack);
        return stack;
    }
	
	@Override
    public void getSubItems(CreativeTabs tab, NonNullList<ItemStack> items)
    {
        if (this.isInCreativeTab(tab))
        {
        	ItemStack stack = new ItemStack(this);
        	this.getCustomization(stack);
            items.add(stack);
        }
    }
}
