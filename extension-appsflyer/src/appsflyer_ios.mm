#if defined(DM_PLATFORM_IOS)


#include <dmsdk/sdk.h>
// AppDelegate.h
#import <AppsFlyerLib/AppsFlyerLib.h>
#include "appsflyer_private.h"
#import "appsflyer_callback_private.h"
#import "DEFAFSDKDelegate.h"
#import "AppsFlyerAttribution.h"
#import "AppsflyerAppDelegate.h"

namespace dmAppsflyer {


struct AppsflyerAppDelegateRegister
{
  AppsflyerAppDelegate* m_Delegate;

    AppsflyerAppDelegateRegister() {
        m_Delegate = [[AppsflyerAppDelegate alloc] init];
        dmExtension::RegisteriOSUIApplicationDelegate(m_Delegate);
    }

    ~AppsflyerAppDelegateRegister() {
        dmExtension::UnregisteriOSUIApplicationDelegate(m_Delegate);
        [m_Delegate release];
    }
};


AppsflyerAppDelegateRegister g_appDelegate;

void Initialize_Ext(){
}

void Finalize_Ext(){
}


void InitializeSDK(const char* key, const char* appleAppID){
             NSLog(@"AppsFlyer InitializeSDK");
  [AppsFlyerLib shared].isDebug = true;
  DEFAFSDKDelegate *delegate = [[DEFAFSDKDelegate alloc] init];
  [AppsFlyerAttribution shared].isBridgeReady = YES;
  [[NSNotificationCenter defaultCenter] postNotificationName:AF_BRIDGE_SET object: [AppsFlyerAttribution shared]];
  [[AppsFlyerLib shared] setAppsFlyerDevKey:[NSString stringWithUTF8String: key]];
  [[AppsFlyerLib shared] setAppleAppID:[NSString stringWithUTF8String: appleAppID]];
  [[AppsFlyerLib shared] setDelegate:delegate];
  [[AppsFlyerLib shared] setDeepLinkDelegate:delegate];
 // [[AppsFlyerLib shared] addPushNotificationDeepLinkPath:@[@"af_push_link"]];
}

void StartSDK(){
  [[AppsFlyerLib shared] start];
}

void SetDebugLog(bool is_debug){
  [AppsFlyerLib shared].isDebug = is_debug;
}

void LogEvent(const char* eventName, dmArray<TrackData>* trackData){
  @autoreleasepool {
    NSMutableDictionary* newDict = [NSMutableDictionary dictionary];
    NSString* key;
    NSString* value;
    NSString* event = [NSString stringWithUTF8String: eventName];
    TrackData data;
    for(uint32_t i = 0; i != trackData->Size(); i++)
    {
      data = (*trackData)[i];
      key = [NSString stringWithUTF8String: data.key];
      value = [NSString stringWithUTF8String: data.value];
      newDict[key] = value;
    }
    [[AppsFlyerLib shared] logEvent: event withValues: newDict];
  }
}


// Fixing build errors by using manual dictionary creation instead of AFAdRevenueData
void LogAdRevenue(const char* monetizationNetwork, const char* mediationNetwork, const char* currencyIso4217Code, double eventRevenue, dmArray<TrackData>* trackData){
    @autoreleasepool {
        NSMutableDictionary* revenueParams = [NSMutableDictionary dictionary];

        // Manually set the keys that AppsFlyer expects for Ad Revenue
        // These match the keys we used in the Android fix
        [revenueParams setValue:@"RewardedVideo" forKey:@"af_adrev_ad_type"];
        [revenueParams setValue:[NSString stringWithUTF8String:currencyIso4217Code] forKey:@"af_currency"];
        [revenueParams setValue:@(eventRevenue) forKey:@"af_revenue"];
        [revenueParams setValue:[NSString stringWithUTF8String:monetizationNetwork] forKey:@"af_adrev_network_name"];
        [revenueParams setValue:[NSString stringWithUTF8String:mediationNetwork] forKey:@"af_adrev_mediation_network_name"];

        // Add any additional parameters passed from Lua
        if (trackData != NULL && trackData->Size() > 0) {
            TrackData data;
            for(uint32_t i = 0; i != trackData->Size(); i++)
            {
                data = (*trackData)[i];
                NSString* key = [NSString stringWithUTF8String: data.key];
                NSString* value = [NSString stringWithUTF8String: data.value];
                revenueParams[key] = value;
            }
        }

        // Send using the standard logEvent API with the reserved "af_ad_revenue" event name
        [[AppsFlyerLib shared] logEvent:@"af_ad_revenue" withValues:revenueParams];
    }
}

void SetCustomerUserId(const char* userId){
  [AppsFlyerLib shared].customerUserID = [NSString stringWithUTF8String: userId];
}

int GetAppsFlyerUID(lua_State* L){
    lua_pushstring(L, [[[AppsFlyerLib shared] getAppsFlyerUID] UTF8String]);
    return 1;
}

 }// namespace

#endif // platform