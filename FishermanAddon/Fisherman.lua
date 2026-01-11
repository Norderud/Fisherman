-- Addon state management
local isEnabled = false

-- Fishing Spell IDs (from BetterFishing)
local FishingIDs = {
    [131474] = true, [131490] = true, [131476] = true, [7620] = true,
    [7731] = true, [7732] = true, [18248] = true, [33095] = true,
    [51294] = true, [88868] = true, [110410] = true, [158743] = true,
    [377895] = true,
}

local isFishing = false
local justCaught = false
local justTooFar = false

-- Create a tiny frame at the top-left of the screen (0,0)
local f = CreateFrame("Frame", "FishermanFrame", UIParent)
f:SetSize(2, 2)
f:SetPoint("TOPLEFT", 0, 0)
f.tex = f:CreateTexture(nil, "OVERLAY")
f.tex:SetAllPoints()

-- Create status indicator frame
local statusFrame = CreateFrame("Frame", "FishermanStatusFrame", UIParent)
statusFrame:SetSize(150, 30)
statusFrame:SetPoint("CENTER", UIParent, "CENTER", 0, 200)
statusFrame:Hide()0

statusFrame.bg = statusFrame:CreateTexture(nil, "BACKGROUND")
statusFrame.bg:SetAllPoints()
statusFrame.bg:SetColorTexture(0, 0, 0, 0.7)

statusFrame.text = statusFrame:CreateFontString(nil, "OVERLAY", "GameFontNormal")
statusFrame.text:SetPoint("CENTER")
statusFrame.text:SetText("Fisherman: ACTIVE")
statusFrame.text:SetTextColor(0, 1, 0)

-- Helper function to update the pixel bridge
local function UpdatePixelBridge()
    if not isEnabled then
        f.tex:SetColorTexture(0, 0, 0)
        return
    end

    -- R: Bags (1 = Full, 0 = OK)
    -- G: Fishing (1 = Casting/Fishing, 0 = Idle)
    -- B: Catch (1 = Just Caught, 0.75 = Too Far, 0.4 = Heartbeat, 0 = No)
    
    local r, g, b = 0, 0, 0

    -- Check Bags
    local freeSlots = 0
    for i = 0, NUM_BAG_SLOTS do
        local bagFreeSlots = C_Container.GetContainerNumFreeSlots(i)
        if bagFreeSlots then
            freeSlots = freeSlots + bagFreeSlots
        end
    end
    if freeSlots == 0 then r = 1 end

    -- Check Fishing
    if isFishing then g = 1 end

    -- Check Catch
    if justCaught then 
        b = 1 
    elseif justTooFar then
        b = 0.75
    else
        b = 0.4 -- Heartbeat / Addon Detected signal
    end

    f.tex:SetColorTexture(r, g, b)
end

-- Helper function to show/hide status indicator
local function UpdateStatusIndicator()
    if isEnabled then
        statusFrame.text:SetText("Fisherman: ACTIVE")
        statusFrame.text:SetTextColor(0, 1, 0)
    else
        statusFrame.text:SetText("Fisherman: INACTIVE")
        statusFrame.text:SetTextColor(1, 0, 0)
    end
    statusFrame:Show()
    C_Timer.After(3, function() statusFrame:Hide() end)
end

-- Slash command handler
local function SlashCommandHandler(msg)
    msg = string.lower(msg or "")
    if msg == "on" or msg == "start" or msg == "enable" then
        isEnabled = true
        UpdatePixelBridge()
        UpdateStatusIndicator()
        print("Fisherman addon: ENABLED")
    elseif msg == "off" or msg == "stop" or msg == "disable" then
        isEnabled = false
        UpdatePixelBridge()
        UpdateStatusIndicator()
        print("Fisherman addon: DISABLED")
    elseif msg == "status" then
        print("Fisherman addon is currently: " .. (isEnabled and "ENABLED" or "DISABLED"))
        UpdateStatusIndicator()
    elseif msg == "stats" then
        if not FishermanStats then
            print("Fisherman statistics not initialized yet.")
            return
        end
        print("Fisherman Statistics:")
        print("  Throws: " .. (FishermanStats.throws or 0))
        print("  Successes: " .. (FishermanStats.successes or 0))
        print("  Looted Items:")
        local hasItems = false
        for item, count in pairs(FishermanStats.items or {}) do
            print("    - " .. item .. ": " .. count)
            hasItems = true
        end
        if not hasItems then
            print("    - None")
        end
    elseif msg == "reset" then
        FishermanStats = { throws = 0, successes = 0, items = {} }
        print("Fisherman Statistics reset.")
    else
        print("Fisherman addon commands: on, off, status, stats, reset")
    end
end

-- Register slash commands
SLASH_FISHERMAN1 = "/fisherman"
SLASH_FISHERMAN2 = "/fish"
SlashCmdList["FISHERMAN"] = SlashCommandHandler

-- Event handler
local function OnEvent(self, event, ...)
    if event == "ADDON_LOADED" then
        local addonName = ...
        if addonName == "Fisherman" or addonName == "FishermanAddon" then
            FishermanStats = FishermanStats or { throws = 0, successes = 0, items = {} }
        end
    end

    if not isEnabled then return end

    if event == "BAG_UPDATE" then
        UpdatePixelBridge()
    elseif event == "UNIT_SPELLCAST_CHANNEL_START" or event == "UNIT_SPELLCAST_CHANNEL_STOP" then
        local unit, _, spellID = ...
        if unit == "player" and FishingIDs[spellID] then
            isFishing = (event == "UNIT_SPELLCAST_CHANNEL_START")
            if isFishing and FishermanStats then
                FishermanStats.throws = FishermanStats.throws + 1
            end
            UpdatePixelBridge()
        end
    elseif event == "CHAT_MSG_LOOT" then
        local msg = ...
        -- Simplistic check for "You receive loot"
        if string.find(msg, "You receive loot") or string.find(msg, "You create") then
             if FishermanStats then
                 if not justCaught then
                     FishermanStats.successes = FishermanStats.successes + 1
                 end
                 
                 -- Try to extract item name from link
                 local itemName = string.match(msg, "|h%[(.-)%]|h")
                 if itemName then
                     FishermanStats.items[itemName] = (FishermanStats.items[itemName] or 0) + 1
                 end
             end
             justCaught = true
             UpdatePixelBridge()
             C_Timer.After(1, function()
                 justCaught = false
                 UpdatePixelBridge()
             end)
        end
    elseif event == "UI_ERROR_MESSAGE" then
        local _, errorMsg = ...
        if errorMsg == ERR_OUT_OF_RANGE then
            justTooFar = true
            UpdatePixelBridge()
            C_Timer.After(2, function()
                justTooFar = false
                UpdatePixelBridge()
            end)
        end
    end
end

-- Register events
f:RegisterEvent("ADDON_LOADED")
f:RegisterEvent("BAG_UPDATE")
f:RegisterEvent("UNIT_SPELLCAST_CHANNEL_START")
f:RegisterEvent("UNIT_SPELLCAST_CHANNEL_STOP")
f:RegisterEvent("CHAT_MSG_LOOT")
f:RegisterEvent("UI_ERROR_MESSAGE")
f:SetScript("OnEvent", OnEvent)

-- Initial status update
UpdatePixelBridge()
UpdateStatusIndicator()