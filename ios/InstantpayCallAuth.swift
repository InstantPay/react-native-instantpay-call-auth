@objc(InstantpayCallAuth)
class InstantpayCallAuth: RCTEventEmitter {

    private var hasListeners = false;
    
    @objc(multiply:withB:withResolver:withRejecter:)
    func multiply(a: Float, b: Float, resolve:RCTPromiseResolveBlock,reject:RCTPromiseRejectBlock) -> Void {
        resolve(a*b)
    }

    // we need to override this method and
    // return an array of event names that we can listen to
    override func supportedEvents() -> [String]! {
        return ["CallAuthDidUpdateState"]
    }
    
    // you also need to add the override attribute
    // on these methods
    override func constantsToExport() -> [AnyHashable: Any] {
        return ["initialCount": 0];
    }
    
    override static func requiresMainQueueSetup() -> Bool {
        return false;
    }
    
    override func startObserving(){
        
        hasListeners = true;
        
        //setup the listener
    }
    
    override func stopObserving(){
        
        hasListeners = false;
        
        //stop the listener
    }
}
