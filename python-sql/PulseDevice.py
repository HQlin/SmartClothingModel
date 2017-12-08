import serial
import time
import sql

startCmd = bytes([0xf0,0xc0,0xb0])
endCmd = bytes([0xf0,0xc1,0xb1])

ser = serial.Serial('/dev/ttyUSB0', 19200)
serble = serial.Serial('/dev/ttyAMA0', 9600, timeout=0.05)                                               
print(ser.name+','+serble.name)

def output_to_ble(ser, data):
	ser.flushInput()
	ser.write(data)
#(datetime.datetime(2016, 2, 28, 16, 36, 7), 100) -> '20160228163607,100'
def changeDatas(day_data):
	return (str(day_data)[2:16] + ',' + str(day_data)[19:]).rstrip(')')

output_to_ble(ser,startCmd)
sql.delect_all_data()
work = 1

try:
	while(1):
		time.sleep(0.1)
		dat = serial.to_bytes(ser.read(5))
        bledat = serial.to_bytes(serble.read(20))
        print(bledat)                                                  
        datLen = len(dat)                                                                                      

        if 'update' in bledat:
			work = 0

        if datLen == 5 and ord(dat[0]) == 240 and ord(dat[1]) == 192:
			pulse = ord(dat[2])*256 + ord(dat[3])
            pulseStr = 'pul:' + str(pulse)

			sql.insert_data(int(pulse),'insert_pul')

			if work == 1:
				output_to_ble(serble, pulseStr)
				print pulseStr
			
			elif work == 0:
				date_time = bledat[6:] #20160101000000
				day_datas = sql.get_day_data("'" + date_time + "'")

				for data in day_datas:
					serble.flushInput()
					serble.write(changeDatas(data)) #20160227000000,100
					print(changeDatas(data))
					bledat = serial.to_bytes(serble.read(6))
					if bledat == 'finish':
						work = 1
						break

				serble.write('finish') #update finish
				sql.delect_all_data()
				print('update finish')
				work = 1

finally:
	ser.write(endCmd)


	