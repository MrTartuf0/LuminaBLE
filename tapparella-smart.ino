#include <ArduinoBLE.h>
#include <ArduinoMotorCarrier.h>

// UUID custom per il Servizio e la Caratteristica BLE
BLEService tapparellaService("19b10000-e8f2-537e-4f6c-d104768a1214");
BLEByteCharacteristic comandoTapparella("19b10001-e8f2-537e-4f6c-d104768a1214", BLEWrite);

// --- PARAMETRI DI CALIBRAZIONE MECCANICA ---
const int ANGOLO_RIPOSO = 90;   // Posizione centrale neutra (non tocca nulla)
const int ANGOLO_APRI = 45;     // Ruota per premere il tasto sotto (Apertura)
const int ANGOLO_CHIUDI = 135;  // Ruota per premere il tasto sopra (Chiusura)

void setup() {
  Serial.begin(115200);

  if (!controller.begin()) {
    Serial.println("Errore: Motor Carrier non trovato!");
    while (1);
  }
  
  if (!BLE.begin()) {
    Serial.println("Errore: Avvio BLE fallito!");
    while (1);
  }

  BLE.setLocalName("Smart_Tapparella");
  BLE.setDeviceName("Smart_Tapparella"); 
  BLE.setAdvertisedService(tapparellaService);
  tapparellaService.addCharacteristic(comandoTapparella);
  BLE.addService(tapparellaService);

  comandoTapparella.writeValue(0);
  BLE.advertise();
  
  // Porta il servo nella posizione centrale di riposo all'avvio
  servo1.setAngle(ANGOLO_RIPOSO);

  Serial.println("Sistema bidirezionale avviato. In attesa di connessione...");
}

void loop() {
  BLEDevice central = BLE.central();

  if (central) {
    Serial.print("Connesso a: ");
    Serial.println(central.address());

    while (central.connected()) {
      
      if (comandoTapparella.written()) {
        int valoreRicevuto = comandoTapparella.value();
        
        // --- CASO 1: COMANDO APRI (Valore 1) ---
        if (valoreRicevuto == 1) {
          Serial.println("Comando RICEVUTO: APRI. Inizio sequenza...");
          
          // Pressione breve sotto
          servo1.setAngle(ANGOLO_APRI);
          delay(500); 
          
          // Rilascio al centro
          servo1.setAngle(ANGOLO_RIPOSO);
          delay(300); 
          
          // Pressione lunga sotto
          servo1.setAngle(ANGOLO_APRI);
          delay(3000); 
          
          // Rilascio finale al centro
          servo1.setAngle(ANGOLO_RIPOSO);
          Serial.println("Apertura completata.");
        }
        
        // --- CASO 2: COMANDO CHIUDI (Valore 2) ---
        else if (valoreRicevuto == 2) {
          Serial.println("Comando RICEVUTO: CHIUDI. Inizio sequenza...");
          
          // Pressione breve sopra
          servo1.setAngle(ANGOLO_CHIUDI);
          delay(500); 
          
          // Rilascio al centro
          servo1.setAngle(ANGOLO_RIPOSO);
          delay(300); 
          
          // Pressione lunga sopra
          servo1.setAngle(ANGOLO_CHIUDI);
          delay(3000); 
          
          // Rilascio finale al centro
          servo1.setAngle(ANGOLO_RIPOSO);
          Serial.println("Chiusura completata.");
        }
        
        // Resetta la caratteristica dopo l'esecuzione
        comandoTapparella.writeValue(0);
      }
    }
    Serial.println("Disconnesso.");
  }
}