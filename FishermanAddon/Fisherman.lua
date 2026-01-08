-- Create a tiny frame at the top-left of the screen (0,0)
local f = CreateFrame("Frame", "SafeFishFrame", UIParent)
f:SetSize(2, 2) -- 2x2 pixels for visibility
f:SetPoint("TOPLEFT", 0, 0)
f.tex = f:CreateTexture(nil, "OVERLAY")
f.tex:SetAllPoints()

-- Logic Loop
local function OnUpdate(self, elapsed)
    local freeSlots = 0
    -- Iterate through Backpack (0) and Bags (1-4)
    for i = 0, 4 do
        freeSlots = freeSlots + C_Container.GetContainerNumFreeSlots(i)
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

f:SetScript("OnUpdate", OnUpdate)