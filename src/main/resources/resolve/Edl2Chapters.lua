---These are globals Resolve gives its embedded Lua scripting state
---@diagnostic disable: undefined-global
--
-- Edl2Chapters bridge. Run it from Workspace > Scripts > Edl2Chapters, then use the app's
-- "Import from DaVinci" button.
--
-- Resolve 21.x sandboxes this state (no io, os.execute, sockets; 21.2 also removed loadfile and
-- SavePrefs), so the app drops request.lua into a mailbox folder, this loop reads it with
-- fu:OpenFile and load(), and answers by writing response.txt with fu:OpenFile. The loop only lives
-- while the app is open: the app keeps an "app-running" file there for as long as its window is.
--
-- This file is a TEMPLATE. The app writes the installed copy into Resolve's Scripts folder at
-- start-up, replacing the placeholder below with the real mailbox folder.

local MAILBOX = [[@MAILBOX@]]

local POLL_SECONDS = 0.2
local STARTUP_GRACE_SECONDS = 180

local REQUEST_FILE = MAILBOX .. "/request.lua"
local RESPONSE_FILE = MAILBOX .. "/response.txt"
local OWNER_FILE = MAILBOX .. "/bridge-owner"
local APP_FILE = MAILBOX .. "/app-running"

local B64 = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"

local fu -- set in run(); file access goes through it because Resolve 21.2 removed io/loadfile/SavePrefs

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

-- Reads a whole text file, or nil. Feature-tested (not version-tested): io, then fu:OpenFile.
local function readFile(path)
    if type(io) == "table" and type(io.open) == "function" then
        local f = io.open(path, "rb")
        if not f then
            return nil
        end
        local text = f:read("*a")
        f:close()
        return text
    end
    if fu ~= nil and type(fu.OpenFile) == "function" then
        local ok, f = pcall(fu.OpenFile, fu, path, 1)
        if not ok or not f then
            return nil
        end
        local lines = {}
        for _ = 1, 100000 do
            local line = f:ReadLine()
            if line == nil then
                break
            end
            lines[#lines + 1] = line
        end
        f:Close()
        return table.concat(lines, "\n")
    end
    return nil
end

-- Overwrites a file with a single line. Returns true on success.
local function writeLine(path, text)
    if type(io) == "table" and type(io.open) == "function" then
        local f = io.open(path, "wb")
        if f then
            f:write(text, "\n")
            f:close()
            return true
        end
    end
    if fu ~= nil and type(fu.OpenFile) == "function" then
        local ok, f = pcall(fu.OpenFile, fu, path, 2)
        if ok and f then
            f:WriteLine(text)
            f:Close()
            return true
        end
    end
    return false
end

local function fileExists(path)
    if type(bmd) == "table" and type(bmd.fileexists) == "function" then
        return bmd.fileexists(path)
    end
    return readFile(path) ~= nil
end

-- Compiles request text with an empty environment: the request is data only, no Lua functions.
local function parseData(text)
    local chunk
    if type(load) == "function" then
        local okLoad, c = pcall(load, text, "request", "t", {})
        chunk = okLoad and c or nil
        if not chunk then -- Lua 5.1 load() takes no env argument
            chunk = load(text, "request")
        end
    end
    if not chunk and type(loadstring) == "function" then
        chunk = loadstring(text, "request")
    end
    if not chunk then
        return nil
    end
    if type(setfenv) == "function" then
        setfenv(chunk, {})
    end
    local ok, value = pcall(chunk)
    return ok and value or nil
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
    local text = readFile(REQUEST_FILE)
    if not text or text == "" then
        return nil
    end
    local request = parseData(text)
    if type(request) == "table" and request.id ~= nil then
        return request
    end
    return nil
end

local function respond(id, body)
    local line = tostring(id) .. ":" .. base64(body)
    for _ = 1, 5 do
        if writeLine(RESPONSE_FILE, line) then
            return
        end
        bmd.wait(0.05)
    end
    print("[Edl2Chapters] Could not write the reply file; this Resolve build blocks file writes.")
end

local function run()
    fu = fusionObject()
    if fu == nil then
        print("[Edl2Chapters] Resolve did not expose Fusion; cannot start the bridge.")
        return
    end

    -- Takeover: a newer run replaces an older loop (which exits when the owner file differs from its token).
    local myToken = tostring(os.time()) .. " " .. tostring({})
    writeLine(OWNER_FILE, myToken)
    bmd.wait(0.6)

    local started = os.time()
    local appSeen = false
    local lastId = nil
    print("[Edl2Chapters] Bridge running. Open Edl2Chapters and press \"Import from DaVinci\".")

    while true do
        local owner = readFile(OWNER_FILE)
        if owner ~= nil and owner:gsub("%s+$", "") ~= myToken then
            print("[Edl2Chapters] Replaced by a newer run; stopping.")
            return
        end
        if fileExists(APP_FILE) then
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
            respond(request.id, ok and ("OK\n" .. result) or ("ERROR\n" .. tostring(result)))
        end
        bmd.wait(POLL_SECONDS)
    end
end

run()
