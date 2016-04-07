package org.impact2585.frc2016.systems;

import org.impact2585.frc2016.Environment;
import org.impact2585.frc2016.RobotMap;
import org.impact2585.frc2016.input.InputMethod;

import edu.wpi.first.wpilibj.DigitalInput;
import edu.wpi.first.wpilibj.Encoder;
import edu.wpi.first.wpilibj.SensorBase;
import edu.wpi.first.wpilibj.SpeedController;
import edu.wpi.first.wpilibj.Talon;
import edu.wpi.first.wpilibj.Victor;
import edu.wpi.first.wpilibj.command.PIDSubsystem;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;


/**
 * Ignore the class name, this is actually an IO shooter
 */
public class IntakeSystem implements RobotSystem, Runnable{
	private InputMethod input;
	private SpeedController wheels;
	private SpeedController leftArm;
	private SpeedController rightArm;
	private SpeedController lever;
	private DigitalInput leftLimitSwitch;
	private DigitalInput rightLimitSwitch;
	private DigitalInput shootingLimitSwitch;
	private IntakePID armPID;
	private boolean isPIDEnabled;
	public static final double ARM_SPEED = 0.3;
	public static final long FORWARD_LEVER_TIME = 250;
	public static final long BACKWARDS_LEVER_TIME = 233;
	private boolean disableSpeedMultiplier;
	private boolean prevSpeedToggle;
	private boolean shooting;
	private Encoder encoder;
	private long startTime;

	/* (non-Javadoc)
	 * @see org.impact2585.frc2016.Initializable#init(org.impact2585.frc2016.Environment)
	 */
	@Override
	public void init(Environment environ) {
		input = environ.getInput();
		wheels = new Talon(RobotMap.INTAKE_WHEEL);
		leftArm = new Talon(RobotMap.INTAKE_LEFT_ARM);
		rightArm = new Talon(RobotMap.INTAKE_RIGHT_ARM);
		rightArm.setInverted(true);
		lever = new Victor(RobotMap.LEVER);
		shootingLimitSwitch = new DigitalInput(RobotMap.SHOOTING_LIMIT_SWITCH);
		leftLimitSwitch = new DigitalInput(RobotMap.LEFT_INTAKE_LIMIT_SWITCH);
		rightLimitSwitch = new DigitalInput(RobotMap.RIGHT_INTAKE_LIMIT_SWITCH);
		disableSpeedMultiplier = false;
		prevSpeedToggle = false;
		isPIDEnabled = false;
		armPID = new IntakePID(.3, .1, 0);
		setEncoder(new Encoder(RobotMap.INTAKE_ARM_ENCODER_PORT_A, RobotMap.INTAKE_ARM_ENCODER_PORT_B));
	}
	
	/**Sets the motors controlling the wheels on intake to speed
	 * @param speed the speed to set the motors to
	 */
	public void spinWheels(double speed) {
		wheels.set(speed);
	}
	
	/**Rotates the intake arm 
	 * @param count the encoder count that the arm should rotate until reached
	 * @param moveTowardBot whether the intake arm should rotate toward the bot
	 */
	public void rotateAngle(int count, boolean moveTowardBot) {
		if(!isPIDEnabled) {
			enableIntakeArmPID(count);
		}
		if(moveTowardBot)
			encoder.setReverseDirection(true);
		if(encoder.get() == count) {
			armPID.getPIDController().reset();
		}
	}
	
	/**Enables the intake arm PID controller
	 * @param count the encoder count to reach
	 */
	public void enableIntakeArmPID(int count) {
		armPID.enable();
		armPID.getSetpoint();
		isPIDEnabled = true;
	}
	
	/**Sets the motors controlling the arms for the intake to speed
	 * @param speed the speed to set the motors to
	 */
	public void moveArms(double speed) {
		leftArm.set(speed);
		rightArm.set(speed);
	}
	
	/**Sets the motor controlling the lever for shooting to speed
	 * @param speed the speed to set the motor to
	 */
	public void spinLever(double speed){
		lever.set(speed);
	}
	
	/**
	 * @returns true if the left limit switch is pressed
	 */
	public boolean isLeftSwitchClosed() {
		return leftLimitSwitch.get();
	}
	
	/**
	 * @returns true if the right limit switch is pressed
	 */
	public boolean isRightSwitchClosed() {
		return rightLimitSwitch.get();
	}
	
	/**
	 * @returns true if the right or left limit switch is closed
	 */
	public boolean isSwitchClosed() {
		return isRightSwitchClosed() || isLeftSwitchClosed();
	}
	
	/**
	 * @returns true if shooting limit switch is pressed 
	 */
	public boolean isShootingSwitchClosed(){
		return shootingLimitSwitch.get();
	}
	
	/**Sets the input of the system to newInput
	 * @param newInput the new input to set to
	 */
	protected void setInput(InputMethod newInput) {
		input = newInput;
	}

	/**
	 * @return the encoder
	 */
	public Encoder getEncoder() {
		return encoder;
	}

	/**
	 * @param encoder the encoder to set
	 */
	public void setEncoder(Encoder encoder) {
		this.encoder = encoder;
	}

	/**
	 * @return the startTime
	 */
	public long getStartTime() {
		return startTime;
	}

	/**
	 * @param startTime the startTime to set
	 */
	protected void setStartTime(long startTime) {
		this.startTime = startTime;
	}
	
	/**
	 * @return the armPID
	 */
	public IntakePID getArmPID() {
		return armPID;
	}

	/**
	 * @param armPID the armPID to set
	 */
	public void setArmPID(IntakePID armPID) {
		this.armPID = armPID;
	}

	/**
	 * Puts the encoder's rate and distance to the SmartDashboard
	 */
	public void accessSmartDashboard() {
		SmartDashboard.putNumber("Intake Arm Rate: ", encoder.getRate());
		SmartDashboard.putNumber("Intake Arm Distance: ", encoder.getDistance());
	}

	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		if(input.intake() && !input.outake()) {
			spinWheels(1);
		} else if(input.outake() && !input.intake()) {
			spinWheels(-1);
		} else {
			spinWheels(0);
		}
		
		double intakeArmSpeed = input.moveIntake();
		if(input.toggleSpeed() && !prevSpeedToggle)
			disableSpeedMultiplier = !disableSpeedMultiplier;
		if(!disableSpeedMultiplier) {
			intakeArmSpeed *= ARM_SPEED;
		}
		
		
		if(input.shoot() && !shooting && !input.turnLeverForward() && !input.turnLeverReverse()){
			shooting = true;
			startTime = System.currentTimeMillis();
		} else if(input.turnLeverForward() && !input.turnLeverReverse() && !input.shoot()) {
			spinLever(0.5);
			shooting = false;
		} else if(input.turnLeverReverse() && !input.turnLeverForward() && !input.shoot()){
			spinLever(-0.5);
			shooting = false;
		} else if(!shooting){
			spinLever(0);
		} 
		if(shooting) {
			shoot();
		}
			
		if(isSwitchClosed() && intakeArmSpeed < 0 && !input.ignoreIntakeLimitSwitch()) {
			intakeArmSpeed = 0;
		}
		
		moveArms(intakeArmSpeed);
		prevSpeedToggle = input.toggleSpeed();
		accessSmartDashboard();
	}
	
	/**
	 * Moves the lever forward for 250 ms, back for 233 ms, and stops if it hits the limit switch
	 */
	public void shoot(){
		if(isShootingSwitchClosed()) {
			shooting = false;
			spinLever(0);
		} else if(System.currentTimeMillis()-startTime < FORWARD_LEVER_TIME){ // if the time that has passed since the lever has started rotating is less than the constant
			spinLever(1.0);
		} else if(System.currentTimeMillis()-startTime >= FORWARD_LEVER_TIME && System.currentTimeMillis() - startTime < FORWARD_LEVER_TIME + BACKWARDS_LEVER_TIME){ // if the time that has passed since the lever has started rotating is more than or equal to the constant
			spinLever(-1.0);
		}  else {
			spinLever(0);
			shooting = false;
		}
	}
	
	/**Sets whether or not the intake system should undergo the timed shoot
	 * @param shoot the boolean value to set shooting to
	 */
	public void setShoot(boolean shoot) {
		shooting = shoot;
	}
	
	/**
	 * @return if the ioshooter is undergoing a timed shoot
	 */
	public boolean isShooting() {
		return shooting;
	}
	
	/* (non-Javadoc)
	 * @see org.impact2585.lib2585.Destroyable#destroy()
	 */
	@Override
	public void destroy() {
		if(wheels instanceof SensorBase) {
			SensorBase motor = (SensorBase) wheels;
			motor.free();
		}
		
		if(lever instanceof SensorBase) {
			SensorBase motor = (SensorBase) lever;
			motor.free();
		}
		
		if(leftArm instanceof SensorBase) {
			SensorBase motor = (SensorBase) leftArm;
			motor.free();
		}
		
		if(rightArm instanceof SensorBase) {
			SensorBase motor = (SensorBase) rightArm;
			motor.free();
		}
		leftLimitSwitch.free();
		rightLimitSwitch.free();
		shootingLimitSwitch.free();
		encoder.free();
		armPID.getPIDController().free();
	}
	
	/**
	 * allows methods inside abstract class PIDSubsystem to be used to move arms 
	 *
	 */
	public class IntakePID extends PIDSubsystem{
		
		/**
		 * @param p proportional constant
		 * @param i integral constant
		 * @param d derivative constant
		 */
		public IntakePID(double p, double i, double d) {
			super(p, i, d);
		}

		/* (non-Javadoc)
		 * @see edu.wpi.first.wpilibj.command.PIDSubsystem#returnPIDInput()
		 */
		@Override
		protected double returnPIDInput(){
			return encoder.get();
		}
		
		/* (non-Javadoc)
		 * @see edu.wpi.first.wpilibj.command.PIDSubsystem#usePIDOutput(double)
		 */
		@Override
		protected void usePIDOutput(double output){
			moveArms(output);
		}

		/* (non-Javadoc)
		 * @see edu.wpi.first.wpilibj.command.Subsystem#initDefaultCommand()
		 */
		@Override
		protected void initDefaultCommand() {
		}
	}
}


