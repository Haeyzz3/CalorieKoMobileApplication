import { useState } from 'react';
import { ChevronRight, Battery, Bluetooth, Shield, Bell, Ruler } from 'lucide-react';
import { BottomNavigation } from './bottom-navigation';
import { Switch } from './ui/switch';

interface SettingsScreenProps {
  onNavigate: (tab: string) => void;
}

export function SettingsScreen({ onNavigate }: SettingsScreenProps) {
  // State for settings
  const [scaleConnected, setScaleConnected] = useState(true);
  const [scaleBattery, setScaleBattery] = useState(87);
  const [dataEncryption, setDataEncryption] = useState(true);
  const [localProcessing, setLocalProcessing] = useState(true);
  const [mealReminders, setMealReminders] = useState(true);
  const [activityAlerts, setActivityAlerts] = useState(false);
  const [useMetricUnits, setUseMetricUnits] = useState(true);
  const [showRecalibrationDialog, setShowRecalibrationDialog] = useState(false);
  const [showWipeDialog, setShowWipeDialog] = useState(false);

  const handleRecalibrate = () => {
    setShowRecalibrationDialog(true);
    // Simulate calibration
    setTimeout(() => {
      setShowRecalibrationDialog(false);
      alert('Scale recalibrated successfully!');
    }, 2000);
  };

  const handleWipeData = () => {
    if (window.confirm('Are you sure you want to wipe all local data? This action cannot be undone.')) {
      setShowWipeDialog(true);
      // Simulate data wipe
      setTimeout(() => {
        setShowWipeDialog(false);
        alert('Local data has been wiped successfully.');
      }, 1500);
    }
  };

  return (
    <div className="h-screen flex flex-col bg-[#F8F9FA]">
      {/* Header */}
      <div className="bg-white border-b border-gray-200 px-6 py-4">
        <h1 className="text-2xl font-semibold text-gray-900">Settings</h1>
      </div>

      {/* Settings Content */}
      <div className="flex-1 overflow-y-auto pb-20">
        {/* Hardware Section */}
        <div className="mt-6">
          <div className="px-6 mb-3">
            <h2 className="text-sm font-semibold text-gray-500 uppercase tracking-wide">
              Hardware (IoT Integration)
            </h2>
          </div>
          <div className="bg-white border-y border-gray-200">
            {/* Scale Connectivity */}
            <div className="flex items-center justify-between px-6 py-4 border-b border-gray-200">
              <div className="flex items-center gap-3">
                <div className={`p-2 rounded-lg ${scaleConnected ? 'bg-green-50' : 'bg-gray-50'}`}>
                  <Bluetooth className={`w-5 h-5 ${scaleConnected ? 'text-[#4CAF50]' : 'text-gray-400'}`} />
                </div>
                <div>
                  <p className="text-sm font-medium text-gray-900">CalorieKo Smart Scale</p>
                  <p className={`text-xs ${scaleConnected ? 'text-[#4CAF50]' : 'text-gray-500'}`}>
                    {scaleConnected ? 'Connected' : 'Disconnected'}
                  </p>
                </div>
              </div>
              <button 
                className="text-gray-400 hover:text-gray-600 transition-colors"
                onClick={() => setScaleConnected(!scaleConnected)}
              >
                <ChevronRight className="w-5 h-5" />
              </button>
            </div>

            {/* Scale Battery */}
            <div className="flex items-center justify-between px-6 py-4 border-b border-gray-200">
              <div className="flex items-center gap-3">
                <div className="p-2 rounded-lg bg-gray-50">
                  <Battery className="w-5 h-5 text-gray-700" />
                </div>
                <div>
                  <p className="text-sm font-medium text-gray-900">Scale Battery</p>
                  <div className="flex items-center gap-2 mt-1">
                    {/* Battery bar */}
                    <div className="w-24 h-2 bg-gray-200 rounded-full overflow-hidden">
                      <div 
                        className={`h-full rounded-full transition-all ${
                          scaleBattery > 50 ? 'bg-[#4CAF50]' : 
                          scaleBattery > 20 ? 'bg-[#FF9800]' : 
                          'bg-red-500'
                        }`}
                        style={{ width: `${scaleBattery}%` }}
                      />
                    </div>
                    <span className="text-xs text-gray-600">{scaleBattery}%</span>
                  </div>
                </div>
              </div>
            </div>

            {/* Calibration */}
            <div className="flex items-center justify-between px-6 py-4">
              <div className="flex-1">
                <p className="text-sm font-medium text-gray-900">Calibration</p>
                <p className="text-xs text-gray-500 mt-1">Recalibrate load cell for accuracy</p>
              </div>
              <button 
                onClick={handleRecalibrate}
                disabled={showRecalibrationDialog}
                className="px-4 py-2 bg-[#4CAF50] text-white text-sm font-medium rounded-lg hover:bg-[#45a049] transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
              >
                {showRecalibrationDialog ? 'Calibrating...' : 'Recalibrate'}
              </button>
            </div>
          </div>
        </div>

        {/* Security & Data Privacy Section */}
        <div className="mt-6">
          <div className="px-6 mb-3">
            <h2 className="text-sm font-semibold text-gray-500 uppercase tracking-wide">
              Security & Data Privacy
            </h2>
          </div>
          <div className="bg-white border-y border-gray-200">
            {/* Data Encryption */}
            <div className="flex items-center justify-between px-6 py-4 border-b border-gray-200">
              <div className="flex items-center gap-3 flex-1">
                <div className="p-2 rounded-lg bg-blue-50">
                  <Shield className="w-5 h-5 text-blue-600" />
                </div>
                <div className="flex-1">
                  <p className="text-sm font-medium text-gray-900">Data Encryption</p>
                  <p className="text-xs text-gray-500 mt-1">SQLCipher AES-256 Encryption</p>
                </div>
              </div>
              <Switch
                checked={dataEncryption}
                onCheckedChange={setDataEncryption}
              />
            </div>

            {/* Edge AI Inference */}
            <div className="flex items-center justify-between px-6 py-4 border-b border-gray-200">
              <div className="flex items-center gap-3 flex-1">
                <div className="p-2 rounded-lg bg-purple-50">
                  <Shield className="w-5 h-5 text-purple-600" />
                </div>
                <div className="flex-1">
                  <p className="text-sm font-medium text-gray-900">Edge AI Inference</p>
                  <p className="text-xs text-gray-500 mt-1">Local Image Processing Only</p>
                </div>
              </div>
              <Switch
                checked={localProcessing}
                onCheckedChange={setLocalProcessing}
              />
            </div>

            {/* Clear Local Data */}
            <div className="px-6 py-4">
              <button 
                onClick={handleWipeData}
                disabled={showWipeDialog}
                className="w-full py-3 bg-red-50 text-red-600 text-sm font-medium rounded-lg hover:bg-red-100 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
              >
                {showWipeDialog ? 'Wiping Data...' : 'Wipe All Local Data'}
              </button>
              <p className="text-xs text-gray-500 text-center mt-2">
                This will delete all locally stored logs and data
              </p>
            </div>
          </div>
        </div>

        {/* Preferences Section */}
        <div className="mt-6 mb-6">
          <div className="px-6 mb-3">
            <h2 className="text-sm font-semibold text-gray-500 uppercase tracking-wide">
              Preferences
            </h2>
          </div>
          <div className="bg-white border-y border-gray-200">
            {/* Notifications Header */}
            <div className="px-6 py-3 bg-gray-50 border-b border-gray-200">
              <div className="flex items-center gap-2">
                <Bell className="w-4 h-4 text-gray-600" />
                <p className="text-sm font-medium text-gray-700">Notifications</p>
              </div>
            </div>

            {/* Meal Reminders */}
            <div className="flex items-center justify-between px-6 py-4 border-b border-gray-200">
              <div className="flex-1">
                <p className="text-sm font-medium text-gray-900">Meal Reminders</p>
                <p className="text-xs text-gray-500 mt-1">Get notified for breakfast, lunch, and dinner</p>
              </div>
              <Switch
                checked={mealReminders}
                onCheckedChange={setMealReminders}
              />
            </div>

            {/* Activity Alerts */}
            <div className="flex items-center justify-between px-6 py-4 border-b border-gray-200">
              <div className="flex-1">
                <p className="text-sm font-medium text-gray-900">Activity Alerts</p>
                <p className="text-xs text-gray-500 mt-1">Reminders to stay active throughout the day</p>
              </div>
              <Switch
                checked={activityAlerts}
                onCheckedChange={setActivityAlerts}
              />
            </div>

            {/* Units Header */}
            <div className="px-6 py-3 bg-gray-50 border-b border-gray-200">
              <div className="flex items-center gap-2">
                <Ruler className="w-4 h-4 text-gray-600" />
                <p className="text-sm font-medium text-gray-700">Units</p>
              </div>
            </div>

            {/* Unit System */}
            <div className="px-6 py-4">
              <div className="flex items-center justify-between mb-2">
                <p className="text-sm font-medium text-gray-900">Measurement System</p>
              </div>
              <div className="flex gap-2">
                <button
                  onClick={() => setUseMetricUnits(true)}
                  className={`flex-1 py-2.5 px-4 rounded-lg text-sm font-medium transition-all ${
                    useMetricUnits
                      ? 'bg-[#4CAF50] text-white shadow-sm'
                      : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
                  }`}
                >
                  Metric (kg / cm)
                </button>
                <button
                  onClick={() => setUseMetricUnits(false)}
                  className={`flex-1 py-2.5 px-4 rounded-lg text-sm font-medium transition-all ${
                    !useMetricUnits
                      ? 'bg-[#4CAF50] text-white shadow-sm'
                      : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
                  }`}
                >
                  Imperial (lbs / in)
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Bottom Navigation */}
      <BottomNavigation activeTab="settings" onTabChange={onNavigate} />
    </div>
  );
}

