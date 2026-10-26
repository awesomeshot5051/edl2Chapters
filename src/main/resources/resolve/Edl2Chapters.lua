---These are globals Resolve gives its embedded Lua scripting state
---@diagnostic disable: undefined-global
--
-- Edl2Chapters bridge. Run it from Workspace > Scripts > Edl2Chapters, then use the app's
-- "Import from DaVinci" button.
--
-- Resolve 21.x sandboxes this state (no io, os.execute, sockets), so it talks to the app the
-- same way AutoSubs does: the app drops request.lua into a mailbox folder, this loop reads it
-- with loadfile, and answers through Fusion.prefs (SetPrefs + SavePrefs). The loop only lives
-- while the app is open: the app keeps an "app-running" file there for as long as its window is.
--
-- This file is a TEMPLATE. The app writes the installed copy into Resolve's Scripts folder at
-- start-up, replacing the placeholder below with the real mailbox folder.

local MAILBOX = [[@MAILBOX@]]

local PREFS = "Global.Edl2ChaptersBridge."
local POLL_SECONDS = 0.2
local STARTUP_GRACE_SECONDS = 180

local REQUEST_FILE = MAILBOX .. "/request.lua"
local APP_FILE = MAILBOX .. "/app-running"

local B64 = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"

local function base64(data)
    local out = {}
    for i = 1, #data, 3 do
        local a, b, c = data:byte(i, i + 2)
        local n = a * 65536 + (b or 0) * 256 + (c or 0)
        local c1 = math.floor(n / 262144) % 64
        local c2 = math.floor(n / 4096) % 64
        local c3 = math.floor(n / 64) % 64
        local c4 = n % 64
        out[#out + 1] = B64:sub(c1 + 1, c1 + 1) .. B64:sub(c2 + 1, c2 + 1)
            .. (b and B64:sub(c3 + 1, c3 + 1) or "=") .. (c and B64:sub(c4 + 1, c4 + 1) or "=")
    end
    return table.concat(out)
end

local function clean(text)
    return (tostring(text):gsub("[\t\r\n]+", " "))
end

local function fusionObject()
    local fu = rawget(_G, "fusion") or rawget(_G, "fu")
    if fu == nil and type(resolve.Fusion) == "function" then
        fu = resolve:Fusion()
    end
    return fu
end

local function getTimelines()
    local project = resolve:GetProjectManager():GetCurrentProject()
    local current = project:GetCurrentTimeline()
    local lines = {}
    for index = 1, project:GetTimelineCount() do -- Resolve indexes timelines from 1
        local timeline = project:GetTimelineByIndex(index)
        local isCurrent = current ~= nil and timeline:GetName() == current:GetName()
        lines[#lines + 1] = (isCurrent and "1" or "0") .. "\t" .. clean(timeline:GetName())
    end
    return table.concat(lines, "\n")
end

local function getMarkers(index)
    local project = resolve:GetProjectManager():GetCurrentProject()
    local timeline = project:GetTimelineByIndex(index)
    if timeline == nil then
        error("no timeline number " .. tostring(index), 0)
    end
    local fps = tonumber(timeline:GetSetting("timelineFrameRate")) or 24
    local frames = {}
    for frame in pairs(timeline:GetMarkers() or {}) do
        frames[#frames + 1] = frame
    end
    table.sort(frames)
    local markers = timeline:GetMarkers()
    local lines = {}
    for _, frame in ipairs(frames) do
        local marker = markers[frame]
        local name = marker.name
        if name == nil or name == "" then
            name = marker.note
        end
        if name == nil or name == "" then
            name = "Marker"
        end
        lines[#lines + 1] = tostring(math.floor(frame / fps)) .. "\t" .. clean(name)
    end
    return table.concat(lines, "\n")
end

local function handle(request)
    if request.action == "timelines" then
        return getTimelines()
    elseif request.action == "markers" then
        return getMarkers(tonumber(request.timeline))
    end
    error("unknown action " .. tostring(request.action), 0)
end

local function readRequest()
    if not bmd.fileexists(REQUEST_FILE) then
        return nil
    end
    local chunk = loadfile(REQUEST_FILE)
    if not chunk then
        return nil
    end
    setfenv(chunk, {}) -- the request is data only: an empty environment, no Lua functions
    local ok, request = pcall(chunk)
    if ok and type(request) == "table" and request.id ~= nil then
        return request
    end
    return nil
end

local function respond(fu, id, body)
    for _ = 1, 5 do -- SavePrefs can fail transiently while Resolve writes the file itself
        local ok = pcall(function()
            fu:SetPrefs(PREFS .. "Response", tostring(id) .. ":" .. base64(body))
            fu:SavePrefs()
        end)
        if ok then
            return
        end
        bmd.wait(0.05)
    end
end

local function run()
    local fu = fusionObject()
    if fu == nil or type(fu.SetPrefs) ~= "function" then
        print("[Edl2Chapters] This Resolve build does not expose Fusion prefs; cannot start the bridge.")
        return
    end

    -- Takeover: a newer run replaces an older loop (which exits when Stop differs from its start value).
    local myStop = tostring(os.time()) .. " " .. tostring({})
    fu:SetPrefs(PREFS .. "Stop", myStop)
    bmd.wait(0.6)

    local started = os.time()
    local appSeen = false
    local lastId = nil
    print("[Edl2Chapters] Bridge running. Open Edl2Chapters and press \"Import from DaVinci\".")

    while true do
        if fu:GetPrefs(PREFS .. "Stop") ~= myStop then
            print("[Edl2Chapters] Replaced by a newer run; stopping.")
            return
        end
        if bmd.fileexists(APP_FILE) then
            appSeen = true
        elseif appSeen then
            print("[Edl2Chapters] App closed; bridge stopped.")
            return
        elseif os.time() - started > STARTUP_GRACE_SECONDS then
            print("[Edl2Chapters] App never opened; bridge stopped.")
            return
        end

        local request = readRequest()
        if request ~= nil and tostring(request.id) ~= lastId then
            lastId = tostring(request.id)
            local ok, result = pcall(handle, request)
            respond(fu, request.id, ok and ("OK\n" .. result) or ("ERROR\n" .. tostring(result)))
        end
        bmd.wait(POLL_SECONDS)
    end
end

run()
