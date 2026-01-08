-- Addon state management
local isEnabled = false  -- Start enabled by default

-- Create a tiny frame at the top-left of the screen (0,0)
local f = CreateFrame("Frame", "FishermanFrame", UIParent)
f:SetSize(2, 2) -- 2x2 pixels for visibility
f:SetPoint("TOPLEFT", 0, 0)
f.tex = f:CreateTexture(nil, "OVERLAY")
f.tex:SetAllPoints()

-- Create status indicator frame
local statusFrame = CreateFrame("Frame", "FishermanStatusFrame", UIParent)
statusFrame:SetSize(150, 30)
statusFrame:SetPoint("CENTER", UIParent, "CENTER", 0, 200)
statusFrame:Hide()  -- Hidden by default

statusFrame.bg = statusFrame:CreateTexture(nil, "BACKGROUND")
statusFrame.bg:SetAllPoints()
statusFrame.bg:SetColorTexture(0, 0, 0, 0.7)

statusFrame.text = statusFrame:CreateFontString(nil, "OVERLAY", "GameFontNormal")
statusFrame.text:SetPoint("CENTER")
statusFrame.text:SetText("Fisherman: ACTIVE")
statusFrame.text:SetTextColor(0, 1, 0)  -- Green text

-- Helper function to show/hide status indicator
local function UpdateStatusIndicator()
    if isEnabled then
        statusFrame.text:SetText("Fisherman: ACTIVE")
        statusFrame.text:SetTextColor(0, 1, 0)  -- Green
        statusFrame:Show()
        -- Auto-hide after 3 seconds
        C_Timer.After(3, function() statusFrame:Hide() end)
    else
        statusFrame.text:SetText("Fisherman: INACTIVE")
        statusFrame.text:SetTextColor(1, 0, 0)  -- Red
        statusFrame:Show()
        -- Auto-hide after 3 seconds
        C_Timer.After(3, function() statusFrame:Hide() end)
    end
end

-- Logic Loop
local function UpdateBagStatus()
    if not isEnabled then
        -- If disabled, set to black/off state
        f.tex:SetColorTexture(0, 0, 0)
        return
    end

    local freeSlots = 0
    -- Iterate through Backpack (0) and all equipped bags (1-4)
    -- Using NUM_BAG_SLOTS is more reliable than GetContainerNumBags
    for i = 0, NUM_BAG_SLOTS do
        local bagFreeSlots = C_Container.GetContainerNumFreeSlots(i)
        -- Add error handling in case the API returns nil
        if bagFreeSlots then
            freeSlots = freeSlots + bagFreeSlots
        end
    end

    if freeSlots == 0 then
        -- Bags Full: Set Color to RED (255, 0, 0)
        f.tex:SetColorTexture(1, 0, 0)
    else
        -- Bags OK: Set Color to BLACK (0, 0, 0)
        -- Using black reduces chance of accidental detection by Java
        f.tex:SetColorTexture(0, 0, 0)
    end
end

-- Slash command handler
local function SlashCommandHandler(msg)
    msg = string.lower(msg or "")

    if msg == "on" or msg == "start" or msg == "enable" then
        isEnabled = true
        UpdateBagStatus()
        UpdateStatusIndicator()
        print("Fisherman addon: ENABLED")
    elseif msg == "off" or msg == "stop" or msg == "disable" then
        isEnabled = false
        UpdateBagStatus()
        UpdateStatusIndicator()
        print("Fisherman addon: DISABLED")
    elseif msg == "status" then
        print("Fisherman addon is currently: " .. (isEnabled and "ENABLED" or "DISABLED"))
        UpdateStatusIndicator()
    else
        print("Fisherman addon commands:")
        print("  /fisherman on/start/enable - Enable the addon")
        print("  /fisherman off/stop/disable - Disable the addon")
        print("  /fisherman status - Show current status")
    end
end

-- Register slash commands
SLASH_FISHERMAN1 = "/fisherman"
SLASH_FISHERMAN2 = "/fish"
SlashCmdList["FISHERMAN"] = SlashCommandHandler

-- Event handler for bag updates
local function OnEvent(self, event, ...)
    if event == "BAG_UPDATE" and isEnabled then
        UpdateBagStatus()
    end
end

-- Register for bag update events
f:RegisterEvent("BAG_UPDATE")
f:SetScript("OnEvent", OnEvent)

-- Initial status update
UpdateBagStatus()
UpdateStatusIndicator()